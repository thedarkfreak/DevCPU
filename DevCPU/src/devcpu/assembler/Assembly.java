package devcpu.assembler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import de.congrace.exp4j.ExpressionBuilder;
import de.congrace.exp4j.UnknownFunctionException;
import de.congrace.exp4j.UnparsableExpressionException;
import devcpu.assembler.exceptions.AbstractAssemblyException;
import devcpu.assembler.exceptions.BadValueException;
import devcpu.assembler.exceptions.DirectiveExpressionEvaluationException;
import devcpu.assembler.exceptions.DuplicateLabelDefinitionException;
import devcpu.assembler.exceptions.OriginBacktrackException;
import devcpu.assembler.exceptions.TooManyRegistersInExpressionException;
import devcpu.assembler.expression.Address;
import devcpu.assembler.expression.Group;
import devcpu.assembler.expression.Register;
import devcpu.emulation.DefaultControllableDCPU;
import devcpu.emulation.FloppyDisk;
import devcpu.emulation.OpCodes;
import devcpu.lexer.Lexer;
import devcpu.lexer.tokens.AValueEndToken;
import devcpu.lexer.tokens.AValueStartToken;
import devcpu.lexer.tokens.AddressEndToken;
import devcpu.lexer.tokens.AddressStartToken;
import devcpu.lexer.tokens.BValueEndToken;
import devcpu.lexer.tokens.BValueStartToken;
import devcpu.lexer.tokens.BasicOpCodeToken;
import devcpu.lexer.tokens.DataToken;
import devcpu.lexer.tokens.DataValueEndToken;
import devcpu.lexer.tokens.DataValueStartToken;
import devcpu.lexer.tokens.ErrorToken;
import devcpu.lexer.tokens.LabelDefinitionToken;
import devcpu.lexer.tokens.LabelToken;
import devcpu.lexer.tokens.LexerToken;
import devcpu.lexer.tokens.LiteralToken;
import devcpu.lexer.tokens.RegisterToken;
import devcpu.lexer.tokens.SimpleStackAccessToken;
import devcpu.lexer.tokens.SpecialOpCodeToken;
import devcpu.lexer.tokens.StringToken;
import devcpu.util.Util;

public class Assembly {
	public static final boolean DEFAULT_LABELS_CASE_SENSITIVE = false;
	public static final String REGISTERS = "ABCXYZIJ";
	private AssemblyDocument rootDocument;
	private ArrayList<AssemblyDocument> documents = new ArrayList<AssemblyDocument>();
	public static boolean labelsCaseSensitive = DEFAULT_LABELS_CASE_SENSITIVE;
	
	public ArrayList<AssemblyLine> lines = new ArrayList<AssemblyLine>();
	public LinkedHashMap<String,Define> defines = new LinkedHashMap<String, Define>();
	public LinkedHashMap<String,LabelDefinition> labelDefs = new LinkedHashMap<String, LabelDefinition>();
	public LinkedHashMap<String,List<LabelUse>> labelUses = new LinkedHashMap<String, List<LabelUse>>();
	private int missed;
	private int shortened;
	private long timer;

	public Assembly(IFile file) throws IOException, CoreException, AbstractAssemblyException {
		rootDocument = new AssemblyDocument(file, this, null);
		documents.add(rootDocument);
	}
	
	//TODO Add Error handling delegate of some sort; Also, these non-AAEs should be caught where they're generated and be handled or throw AAEs
	public void assemble(DefaultControllableDCPU dcpu) throws AbstractAssemblyException, UnknownFunctionException, UnparsableExpressionException, IOException, CoreException {
		timerStart();
		rootDocument.readLines();
		System.out.println(timerReset() + "ms in Line Loading");
		preprocessAndSize(true);
		System.out.println(timerReset() + "ms in Preprocessing");
		//TODO More passes
		zeroBuffer(dcpu.ram);
		System.out.println(timerReset() + "ms to zero RAM");
		assembleToBuffer(dcpu.ram);
		System.out.println(timerEnd() + "ms in Final Assembly");
	}
	
	public void assemble(FloppyDisk disk) throws AbstractAssemblyException, UnknownFunctionException, UnparsableExpressionException, IOException, CoreException {
		timerStart();
		rootDocument.readLines();
		System.out.println(timerReset() + "ms in Line Loading");
		preprocessAndSize(true);
		//TODO More passes
		System.out.println(timerReset() + "ms in Preprocessing");
		zeroBuffer(disk.data);
		System.out.println(timerReset() + "ms to zero disk data");
		assembleToBuffer(disk.data);
		System.out.println(timerEnd() + "ms in Final Assembly");
	}

	private void preprocessAndSize(boolean preprocess) throws AbstractAssemblyException {
		//Note: Label collection can be done here now, but directives added later could necessitate
		//moving this until after all preprocessing is done.
		int oMin = 0;
		int oMax = 0;
		boolean exact = true;
		LinkedHashMap<Pattern,Define> patterns = new LinkedHashMap<Pattern, Define>();
		for (String key : defines.keySet()) {
			patterns.put(Pattern.compile("\\b"+Pattern.quote(key)+"\\b"), defines.get(key));
		}
		String lastDefinedGlobalLabel = null;
		for (AssemblyLine line : lines) {
			if (exact) {
				line.offset = oMin;
				line.located = true;
			} else {
				line.minOffset = oMin;
				line.maxOffset = oMax;
			}
			if (preprocess) {
				String pass = "";
				boolean isDefine = false;
				if (line.isDirective()) {
					if (line.getDirective().isDefine()) {
						isDefine = true;
						pass = new Define(line.getDirective()).getKey();
					}
				}
				boolean retokenize = false;
				String text = line.getText();
				for (Pattern pattern : patterns.keySet()) {
					if (isDefine) {
						if (patterns.get(pattern).getDirective().getLine().equals(line)) {
							continue;
						}
					}
					if (pattern.matcher(text).find()) {
						retokenize = true;
						text = text.replaceAll(pattern.pattern(), patterns.get(pattern).getValue());
					}
				}
				if (retokenize) {
					if (isDefine) {
						defines.get(pass).setValue(Define.extractValue(text));
					}
					line.setProcessedTokens(Lexer.get().generateTokens(text, true));
				}
				line.preprocess();
				for (LexerToken token : line.getProcessedTokens()) {
					if (token instanceof LabelDefinitionToken) {
						LabelDefinition labelDef = new LabelDefinition(line, (LabelDefinitionToken) token, labelsCaseSensitive, lastDefinedGlobalLabel);
						((LabelDefinitionToken)token).labelDef = labelDef;
						if (!labelDef.isLocal()) {
							lastDefinedGlobalLabel = labelDef.getLabelName();
						}
						if (labelDefs.containsKey(labelDef.getLabelName())) {
							throw new DuplicateLabelDefinitionException(this, labelDefs.get(labelDef.getLabelName()),labelDef);
						}
						labelDefs.put(labelDef.getLabelName(), labelDef);
						if (exact) {
							if (labelUses.containsKey(labelDef.getLabelName())) {
								for (LabelUse use : labelUses.get(labelDef.getLabelName())) {
									use.getToken().value = line.offset;
									use.getToken().valueSet = true;
									use.getLine().unvaluedLabelTokens--;
								}
								labelUses.remove(labelDef.getLabelName());
							}
						}
					} else if (token instanceof LabelToken) {
						LabelUse labelUse = new LabelUse(line, (LabelToken) token, labelsCaseSensitive, lastDefinedGlobalLabel);
						((LabelToken) token).labelName = labelUse.getLabelName();
						if (labelDefs.containsKey(labelUse.getLabelName()) && labelDefs.get(labelUse.getLabelName()).getLine().located) {
							((LabelToken) token).value = labelDefs.get(labelUse.getLabelName()).getLine().offset; //Setting it early
							((LabelToken) token).valueSet = true;
							line.unvaluedLabelTokens--;
						} else {
							if (!labelUses.containsKey(labelUse.getLabelName())) {
								labelUses.put(labelUse.getLabelName(), new ArrayList<LabelUse>());
							}
							labelUses.get(labelUse.getLabelName()).add(labelUse);
						}
					} else if (token instanceof ErrorToken) {
						//TODO Throw exception
						System.err.println("Error tokenizing line " + line.getLineNumber() + " in " + line.getDocument().getFile().getName() + ": " + line.getText());
					}
					//TODO: Additional validity checks?
				}
			} else {
				if (line.unvaluedLabelTokens != 0) {
					for (LexerToken token : line.getProcessedTokens()) {
						if (token instanceof LabelToken) {
							if (!((LabelToken) token).valueSet) {
								if (labelDefs.get(((LabelToken) token).labelName).getLine().located) {
									((LabelToken) token).value = labelDefs.get(((LabelToken) token).labelName).getLine().offset; //Setting it early
									((LabelToken) token).valueSet = true;
									line.unvaluedLabelTokens--;
								}	
							}
						} else if (token instanceof LabelDefinitionToken) {
							if (exact) {
								LabelDefinition labelDef = ((LabelDefinitionToken) token).labelDef;
								if (labelUses.containsKey(labelDef.getLabelName())) {
									for (LabelUse use : labelUses.get(labelDef.getLabelName())) {
										use.getToken().value = line.offset;
										use.getToken().valueSet = true;
										use.getLine().unvaluedLabelTokens--;
									}
									labelUses.remove(labelDef.getLabelName());
								}
							}
						}
					}
				}
			}
			if (!line.sized) {// || !line.located) {
				if (line.isDirective()) {
					Directive directive = line.getDirective();
					if (directive.isOrigin()) {
//						if (!line.sized) {
							if (line.nextOffset == 0) {
								try {
									line.nextOffset = (int) new ExpressionBuilder(decimalize(directive.getParametersToken().getText())).build().calculate();
								} catch (Exception e) {
									throw new DirectiveExpressionEvaluationException(directive);
								}
							}
							if (exact) {
								if (line.nextOffset < oMin) {
									throw new OriginBacktrackException(directive);
								}
								line.size = line.nextOffset - oMin;
								line.sized = true;
							}
//						}
						oMin = line.nextOffset;
						oMax = line.nextOffset;
					} else if (directive.isAlign()) {
//						if (!line.sized) {
							if (line.nextOffset == 0) {
								try {
									line.nextOffset = (int) new ExpressionBuilder(decimalize(directive.getParametersToken().getText())).build().calculate();
								} catch (Exception e) {
									throw new DirectiveExpressionEvaluationException(directive);
								}
							}
							if (exact) {
								if (line.nextOffset < oMin) {
									throw new OriginBacktrackException(directive);
								}
								line.size = line.nextOffset - oMin;
								line.sized = true;
							}
//						}
						oMin = line.nextOffset;
						oMax = line.nextOffset;
					} else if (directive.isFill()) {
//						if (!line.sized) {
							try {
								LexerToken[] paramTokens = Lexer.get().generateTokens("SET " + directive.getParametersToken().getText(), true);
								String spanText = "";
								int i = 0;
								while (!(paramTokens[++i] instanceof BValueStartToken)) {}
								while (!(paramTokens[++i] instanceof BValueEndToken)) {spanText += paramTokens[i].getText();}
								line.size = (int) new ExpressionBuilder(decimalize(spanText)).build().calculate();
								line.sized = true;
							} catch (Exception e) {
								throw new DirectiveExpressionEvaluationException(directive);
							}
							if (line.size < 0) {
								throw new OriginBacktrackException(directive);
							}
//						}
						oMin += line.size;
						oMax += line.size;
					} else if (directive.isReserve()) {
//						if (!line.sized) {
							try {
								line.size = (int) new ExpressionBuilder(decimalize(directive.getParametersToken().getText())).build().calculate();
								line.sized = true;
							} catch (Exception e) {
								throw new DirectiveExpressionEvaluationException(directive);
							}
							if (line.size < 0) {
								throw new OriginBacktrackException(directive);
							}
//						}
						oMin += line.size;
						oMax += line.size;
					}
				} else {
					
					//TODO START HERE ******************
					int size = 0;
					LexerToken[] tokens = line.getProcessedTokens();
					for (int i = 0; i < tokens.length; i++) {
						LexerToken token = tokens[i];
						if (token instanceof BasicOpCodeToken) {
							size++; //1
							//Check if the b Value is a simple stack accessor or register
							if (line.bIsAddress) {
								if (line.bRegister == null || line.bHasOperator) {
									size++;
									((BasicOpCodeToken)token).setBValueNextWord(true);
								} else if ("SP".equals(line.bRegister)) {
									size++;
									((BasicOpCodeToken)token).setBValueNextWord(true);
								}
								i += 4;
							} else if (tokens[(i+=2)] instanceof LiteralToken) {
								if (!line.bHasOperator) {
									line.literalB = (char) (((LiteralToken)tokens[i]).getValue() & 0xFFFF);
									line.literalBSet = true;
								}
								size++; //2
								((BasicOpCodeToken)token).setBValueNextWord(true);
							} else if (tokens[i] instanceof RegisterToken) {
								if (!(tokens[i+1] instanceof BValueEndToken)) { //Change to ++i?
									size++;
									((BasicOpCodeToken)token).setBValueNextWord(true);
								}
							} else if (tokens[i] instanceof AddressStartToken) {
								if (tokens[++i] instanceof RegisterToken) {
									if (!(tokens[++i] instanceof AddressEndToken)) {
										size++;
										((BasicOpCodeToken)token).setBValueNextWord(true);
									}
								} else {
									size++;
									((BasicOpCodeToken)token).setBValueNextWord(true);
								}
							} else if (tokens[i] instanceof SimpleStackAccessToken) {
							} else if (tokens[i] instanceof LabelToken && !line.bHasOperator) {
								if (((LabelToken)tokens[i]).valueSet) {
									line.literalB = (char) ((LabelToken)tokens[i]).value;
									line.literalBSet = true;
								}
								size++;
								((BasicOpCodeToken)token).setAValueNextWord(true);
							} else {
								size++;
								((BasicOpCodeToken)token).setBValueNextWord(true);
							}
							while (!(tokens[++i] instanceof AValueStartToken)) {}
							//Check the a Value (can also be a non-expression non-label literal and meet short literal requirements)
							if (line.aIsAddress) {
								if (line.aRegister == null || line.aHasOperator) {
									size++;
									((BasicOpCodeToken)token).setAValueNextWord(true);
								} else if ("SP".equals(line.aRegister)) {
									size++;
									((BasicOpCodeToken)token).setAValueNextWord(true);
								}
								i += 3;
							} else if (tokens[++i] instanceof LiteralToken) {
								if (tokens[i+1] instanceof AValueEndToken) {
									char val = (char) (((LiteralToken)tokens[i]).getValue() & 0xFFFF);
									if (val >= 31 && val != 0xFFFF) {
										size++;
										line.literalA = val;
										line.literalASet = true;
										((BasicOpCodeToken)token).setAValueNextWord(true);
									}
								} else {
									size++;
									((BasicOpCodeToken)token).setAValueNextWord(true);
								}
							} else if (tokens[i] instanceof RegisterToken) {
								if (!(tokens[i+1] instanceof AValueEndToken)) {
									size++;
									((BasicOpCodeToken)token).setAValueNextWord(true);
								}
							} else if (tokens[i] instanceof AddressStartToken) {
								if (tokens[++i] instanceof RegisterToken) {
									if (!(tokens[++i] instanceof AddressEndToken)) {
										size++;
										((BasicOpCodeToken)token).setAValueNextWord(true);
									}
								} else {
									size++;
									((BasicOpCodeToken)token).setAValueNextWord(true);
								}
							} else if (tokens[i] instanceof SimpleStackAccessToken) {
							} else if (tokens[i] instanceof LabelToken && !line.aHasOperator) {
								if (((LabelToken)tokens[i]).valueSet) {
									char val = (char) ((LabelToken)tokens[i]).value;
									if (val >= 31 && val != 0xFFFF) {
										size++;
										line.literalA = val;
										line.literalASet = true;
										((BasicOpCodeToken)token).setAValueNextWord(true);
									}
								} else {
									size++;
									((BasicOpCodeToken)token).setAValueNextWord(true);
								}
							} else {
								size++;
								((BasicOpCodeToken)token).setAValueNextWord(true);
							}
						} else if (token instanceof SpecialOpCodeToken) {
							size++;
							if (line.aIsAddress) {
								if (line.aRegister == null || line.aHasOperator) {
									size++;
									((SpecialOpCodeToken)token).setAValueNextWord(true);
								} else if ("SP".equals(line.aRegister)) {
									size++;
									((SpecialOpCodeToken)token).setAValueNextWord(true);
								}
								i += 4;
							} else if (tokens[(i+=2)] instanceof LiteralToken) {
								if (tokens[i+1] instanceof AValueEndToken) {
									char val = (char) (((LiteralToken)tokens[i]).getValue() & 0xFFFF);
									if (val >= 31 && val != 0xFFFF) {
										size++;
										line.literalA = val;
										line.literalASet = true;
										((SpecialOpCodeToken)token).setAValueNextWord(true);
									}
								} else {
									size++;
									((SpecialOpCodeToken)token).setAValueNextWord(true);
								}
							} else if (tokens[i] instanceof RegisterToken) {
								if (!(tokens[i+1] instanceof AValueEndToken)) {
									size++;
									((SpecialOpCodeToken)token).setAValueNextWord(true);
								}
							} else if (tokens[i] instanceof AddressStartToken) {
								if (tokens[++i] instanceof RegisterToken) {
									if (!(tokens[++i] instanceof AddressEndToken)) {
										size++;
										((SpecialOpCodeToken)token).setAValueNextWord(true);
									}
								} else {
									size++;
									((SpecialOpCodeToken)token).setAValueNextWord(true);
								}
							} else if (tokens[i] instanceof SimpleStackAccessToken) {
							} else if (tokens[i] instanceof LabelToken && !line.aHasOperator) {
								if (((LabelToken)tokens[i]).valueSet) {
									char val = (char) ((LabelToken)tokens[i]).value;
									if (val >= 31 && val != 0xFFFF) {
										size++;
										line.literalA = val;
										line.literalASet = true;
										((SpecialOpCodeToken)token).setAValueNextWord(true);
									}
								} else { //What else?
									size++;
									((SpecialOpCodeToken)token).setAValueNextWord(true);
								}
							} else {
								size++;
								((SpecialOpCodeToken)token).setAValueNextWord(true);
							}
						} else if (token instanceof DataToken) {
							i++;
							while (i < tokens.length && tokens[i] instanceof DataValueStartToken) {
								token = tokens[++i];
								if (!(tokens[i+1] instanceof DataValueEndToken)) {
									size++;
									while (!(tokens[++i] instanceof DataValueEndToken)) {}
								} else {
									if (token instanceof StringToken) {
										//TODO: Decide whether strings should default to packed or not (currently they are not, and non-ascii characters are allowed in the string)
										size += ((StringToken)token).getString().length();
									} else {
										size++;
									}
									i++;
								}
								i++;
							}
						}
					}
					line.size = size;//TODO
					line.sized = true;
					oMin += line.size;
					oMax += line.size;
					
	//					System.out.println(line.getOffset() + ": (" + line.getSize() + ") " + line.getText());
				}
			} else {
				oMin += line.size;
				oMax += line.size;
			}
			exact = oMin == oMax;
		}
	}

	private void assembleToBuffer(char[] ram) throws AbstractAssemblyException, UnknownFunctionException, UnparsableExpressionException {
		int pc = 0;
		int opCode;
		int a;
		int b;
		for (AssemblyLine line : lines) {
			pc = line.offset;
			if (line.isDirective()) {
				Directive directive = line.getDirective();
				if (directive.isAlign()) {
					int end = pc + line.size;
					while (pc < end) {
						ram[pc++] = 0;
					}
				} else if (directive.isReserve()) {
					int end = pc + line.size;
					while (pc < end) {
						ram[pc++] = 0;
					}
				} else if (directive.isFill()) {
					//TODO This is hackish. Make it not hackish.
					LexerToken[] paramTokens = Lexer.get().generateTokens("SET " + directive.getParametersToken().getText(), true);
					String valText = "";
					int i = 0;
					while (!(paramTokens[++i] instanceof AValueStartToken)) {}
					while (!(paramTokens[++i] instanceof AValueEndToken)) {valText += paramTokens[i].getText();}
					char v = (char)(int) new ExpressionBuilder(decimalize(valText)).build().calculate();
					int end = pc + line.size;
					while (pc < end) {
						ram[pc++] = v;
					}
				}
			} else {
				LexerToken[] tokens = line.getProcessedTokens();
				for (int i = 0; i < tokens.length; i++) {
					LexerToken token = tokens[i];
					if (token instanceof SpecialOpCodeToken) {
						opCode = OpCodes.special.getId(token.getText().toUpperCase());
						a = getA(tokens,i+1,((SpecialOpCodeToken)token).isNextWordA()?pc+1:0,ram);
						ram[pc] = (char)(opCode << 5 | a << 10);
					} else if (token instanceof BasicOpCodeToken) {
						opCode = OpCodes.basic.getId(token.getText().toUpperCase());
						a = getA(tokens,i+1,((BasicOpCodeToken)token).isNextWordA()?pc+1:0,ram);
						b = getB(tokens,i+1,((BasicOpCodeToken)token).isNextWordB()?((BasicOpCodeToken)token).isNextWordA()?pc+2:pc+1:0,ram);
						ram[pc] = (char)(opCode | b << 5 | a << 10);
					} else if (token instanceof DataValueStartToken) {
						pc = assembleData(tokens, i+1, ram, pc);
					}
				}
			}
		}
	}

	private int assembleData(LexerToken[] tokens, int i, char[] ram, int pc) throws UnknownFunctionException, UnparsableExpressionException {
		LexerToken token = tokens[i];
		if (token instanceof StringToken) {
			return assembleString(((StringToken) token).getString(), ram, pc);
		} else {
			return assembleExpression(tokens, i, ram, pc);
		}
	}

	private int assembleExpression(LexerToken[] tokens, int i, char[] ram, int pc) throws UnknownFunctionException, UnparsableExpressionException {
		String expression = "";
		while (!(tokens[i] instanceof DataValueEndToken)) {
			LexerToken token = tokens[i++];
			if (token instanceof LabelToken) { //Wait, is this even possible at this point...
				expression += ""+((LabelToken)token).value;
			} else if (token instanceof LiteralToken) {
				expression += ""+((LiteralToken)token).getValue();
			} else {
				expression += token.getText();
			}
		}
		int val = (int) new ExpressionBuilder(expression).build().calculate();
		ram[pc++] = (char) val;
		return pc;
	}

	private int assembleString(String string, char[] ram, int pc) {
		for (char c : string.toCharArray()) {
			ram[pc++] = c;
		}
		return pc;
	}

	private int getB(LexerToken[] tokens, int i, int offset, char[] ram) throws AbstractAssemblyException, UnknownFunctionException, UnparsableExpressionException {
		boolean isAddress;
		boolean isExpression;
		boolean hasSimpleStackAccessor;
		String register = "";
		boolean hasNextWord = offset > 0;
		
		while (!(tokens[i] instanceof BValueStartToken)) {i++;}
		Group value = null;
		if (tokens[i+1] instanceof AddressStartToken) {
			value = new Address(tokens,i+1,AddressEndToken.class);
			isAddress = true;
		} else {
			value = new Group(tokens,i,BValueEndToken.class);	
			isAddress = false;
		}
		
		isExpression = value.isExpression();
		hasSimpleStackAccessor = value.hasSimpleStackAccessor();
		List<Register> registers = value.getRegisters();
		//Register validity checks
		if (registers.size() > 1) {
			throw new TooManyRegistersInExpressionException(this, registers, tokens, "b");
		} else if (registers.size() == 1) {
			register = registers.get(0).getRegister();
			if (register.equals("EX") || register.equals("PC")) {
				if (isAddress || isExpression) {
					throw new BadValueException(this, tokens, register + " used in an address or expression.");
				}
			}
			if (!isAddress && isExpression) {
				throw new BadValueException(this, tokens, register + " used in an expression outside of an address."); 
			}
			if (value.scanForRegistersInUnaryOperations()) {
				throw new BadValueException(this, tokens, register + " used in a unary operation.");
			}
			if (value.scanForRegistersBeingSubtracted()) {
				throw new BadValueException(this, tokens, register + " subtracted.");
			}
			if (value.scanForRegistersInDisallowedOperations()) {
				throw new BadValueException(this, tokens, register + " used in disallowed operation.");
			}
		}
		int literal = (int) new ExpressionBuilder(value.getExpression()).build().calculate(); 
		
		//SSA validity checks
		if (hasSimpleStackAccessor && (isAddress || isExpression)) {
			throw new BadValueException(this, tokens, value.getSimpleStackAccessor().getAccessor() + " used in an address or expression.");
		}
		
		if (hasNextWord) {
			ram[offset] = (char) literal;
			if (isAddress) {
				if (registers.size() == 1) {
					if (register.equals("SP")) { //0x1a | [SP + next word] / PICK n
						return 0x1a;
					} else { //0x10-0x17 | [register + next word]
						return 0x10 + REGISTERS.indexOf(register);
					}
				} else { //0x1e | [next word]
					return 0x1e;
				}
			} else if (value.hasPickValue()) { //0x1a | [SP + next word] / PICK n
				return 0x1a;
			} else {//0x1f | next word (literal)
				return 0x1f;
			}
		} else { //Not next word
			if (isAddress) {
//				if (register.equals("SP")) { //0x19 | [SP] / PEEK
//					return 0x19; //TODO FIXME I think this is actually guaranteed to be a simple stack accessor
//				} else { //0x08-0x0f | [register]
				return 0x08 + REGISTERS.indexOf(register);
//				}
			} else {
				if (registers.size() == 1) {
					if (register.equals("SP")) { //0x1b | SP
						return 0x1b;
					}
					if (register.equals("PC")) { //0x1c | PC
						return 0x1c;
					}
					if (register.equals("EX")) { //0x1d | EX
						return 0x1d;
					}
					//0x00-0x07 | register (A, B, C, X, Y, Z, I or J, in that order)
					return REGISTERS.indexOf(register);
				} else if (hasSimpleStackAccessor) {
					String accessor = value.getSimpleStackAccessor().getAccessor();
					if (accessor.equals("PUSH") || accessor.equals("[--SP]")) { //0x18 | (PUSH / [--SP]) if in b, or (POP / [SP++]) if in a
						return 0x18;
					}
					if (accessor.equals("PEEK") || accessor.equals("[SP]")) { //0x19 | [SP] / PEEK
						return 0x19;
					}
				}
			}
		}
		//Disallowed value conditions:
		//1.  An operand for an operation other than addition or subtraction is or contains a register.
		//2.  The right operand for a subtraction is or contains a register.
		//3.  The operand for a unary operation is or contains a register. (allow even number of negations? Not for now. Screw weird people)
		//4. 	More than one register token exists in value
		//5. 	Register token exists in expression outside of address
		//6. 	PC or EX used in expression or address
		//7.  Simple stack accessor used in expression or address
		
		//After ruling out those conditions, all of which are invalid and should throw exceptions,
		//you can replace any register with a zero, construct a string out of the expression, 
		//and run it through exp4j to get the literal value. That literal value will be valid in 
		//all value conditions. The presence of a register, combined with the value in offset and 
		//whether it's an address value, is enough information to determine with certainty which 
		//value to return (for use in the opcode).
		
		//TODO Exception
		System.out.println("Didn't find its value.");
		return 0;
	}

	private int getA(LexerToken[] tokens, int i, int offset, char[] ram) throws AbstractAssemblyException, UnknownFunctionException, UnparsableExpressionException {
		boolean isAddress;
		boolean isExpression;
		boolean hasSimpleStackAccessor;
		String register = "";
		boolean hasNextWord = offset > 0;
		
		while (!(tokens[i] instanceof AValueStartToken)) {i++;}
		Group value = null;
		if (tokens[i+1] instanceof AddressStartToken) {
			value = new Address(tokens,i+1,AddressEndToken.class);
			isAddress = true;
		} else {
			value = new Group(tokens,i,AValueEndToken.class);	
			isAddress = false;
		}
		
		isExpression = value.isExpression();
		hasSimpleStackAccessor = value.hasSimpleStackAccessor();
		List<Register> registers = value.getRegisters();
		//Register validity checks
		if (registers.size() > 1) {
			throw new TooManyRegistersInExpressionException(this, registers, tokens, "a");
		} else if (registers.size() == 1) {
			register = registers.get(0).getRegister();
			if (register.equals("EX") || register.equals("PC")) {
				if (isAddress || isExpression) {
					throw new BadValueException(this, tokens, register + " used in an address or expression.");
				}
			}
			if (!isAddress && isExpression) {
				throw new BadValueException(this, tokens, register + " used in an expression outside of an address."); 
			}
			if (value.scanForRegistersInUnaryOperations()) {
				throw new BadValueException(this, tokens, register + " used in a unary operation.");
			}
			if (value.scanForRegistersBeingSubtracted()) {
				throw new BadValueException(this, tokens, register + " subtracted.");
			}
			if (value.scanForRegistersInDisallowedOperations()) {
				throw new BadValueException(this, tokens, register + " used in disallowed operation.");
			}
		}
		int literal = (int) new ExpressionBuilder(value.getExpression()).build().calculate(); 
		
		//SSA validity checks
		if (hasSimpleStackAccessor && (isAddress || isExpression)) {
			throw new BadValueException(this, tokens, value.getSimpleStackAccessor().getAccessor() + " used in an address or expression.");
		}
		
		if (hasNextWord) {
			ram[offset] = (char) literal;
			if (isAddress) {
				if (registers.size() == 1) {
					if (register.equals("SP")) { //0x1a | [SP + next word] / PICK n
						return 0x1a;
					} else { //0x10-0x17 | [register + next word]
						return 0x10 + REGISTERS.indexOf(register);
					}
				} else { //0x1e | [next word]
					return 0x1e;
				}
			} else if (value.hasPickValue()) { //0x1a | [SP + next word] / PICK n
				return 0x1a;
			} else {//0x1f | next word (literal)
				if (ram[offset] < 31 || ram[offset] == 0xFFFF) {
					missed++;
				}
				return 0x1f;
			}
		} else { //Not next word
			if (isAddress) {
				return 0x08 + REGISTERS.indexOf(register);
			} else {
				if (registers.size() == 1) {
					if (register.equals("SP")) { //0x1b | SP
						return 0x1b;
					}
					if (register.equals("PC")) { //0x1c | PC
						return 0x1c;
					}
					if (register.equals("EX")) { //0x1d | EX
						return 0x1d;
					}
					return REGISTERS.indexOf(register); //0x00-0x07 | register (A, B, C, X, Y, Z, I or J, in that order)
				} else if (hasSimpleStackAccessor) {
					String accessor = value.getSimpleStackAccessor().getAccessor();
					if (accessor.equals("POP") || accessor.equals("[SP++]")) { //0x18 | (PUSH / [--SP]) if in b, or (POP / [SP++]) if in a
						return 0x18;
					}
					if (accessor.equals("PEEK") || accessor.equals("[SP]")) { //0x19 | [SP] / PEEK
						return 0x19;
					}
				} else { //0x20-0x3f | literal value 0xffff-0x1e (-1..30) (literal) (only for a)
					shortened++;
					return 0x21 + literal;
				}
			}
		}
		//Disallowed value conditions:
		//1.  An operand for an operation other than addition or subtraction is or contains a register.
		//2.  The right operand for a subtraction is or contains a register.
		//3.  The operand for a unary operation is or contains a register. (allow even number of negations? Not for now. Screw weird people)
		//4. 	More than one register token exists in value
		//5. 	Register token exists in expression outside of address
		//6. 	PC or EX used in expression or address
		//7.  Simple stack accessor used in expression or address
		
		//After ruling out those conditions, all of which are invalid and should throw exceptions,
		//you can replace any register with a zero, construct a string out of the expression, 
		//and run it through exp4j to get the literal value. That literal value will be valid in 	
		//all value conditions. The presence of a register, combined with the value in offset and 
		//whether it's an address value, is enough information to determine with certainty which 
		//value to return (for use in the opcode).
		
		//TODO Exception
		System.out.println("Didn't find its value.");
		return 0;
	}

	private void zeroBuffer(char[] buf) {
		for (int i = 0; i < buf.length; i++) {
			buf[i] = 0;
		}
	}

	private String decimalize(String text) {
		//TODO Document that character literals (i.e. 1+'a' <--) are not allowed in directive parameter expressions
		String[] s = text.split("\\b");
		for (int i = 0; i < s.length; i++) {
			if (s[i].length() > 0) {
				if (s[i].startsWith("0x")) {
					s[i] = ""+Integer.parseInt(s[i].substring(2), 16);
		    } else if (s[i].startsWith("0b")) {
		    	s[i] = ""+ Integer.parseInt(s[i].substring(2), 2);
	//	    } else if (v.startsWith("'") && text.endsWith("'") && text.length()==3) {
	//				val = text.charAt(1);
				}
			}
		}
		return Util.join(s, ' ');
	}

	public int getFileCount() {
		return 1 + treeCountChildren(rootDocument);
	}

	private int treeCountChildren(AssemblyDocument doc) {
		int n = 0;
		for (AssemblyDocument child : doc.getChildren().values()) {
			n += 1 + treeCountChildren(child);
		}
		return n;
	}

	public int getLineCount() {
		return lines.size();
	}

	public int getSize() {
		AssemblyLine lastLine = lines.get(lines.size()-1);
		return lastLine.offset + lastLine.size;
	}
	
	public int getMissedShortLiteralEstimate() {
		return missed;
	}
	
	public int getAssembledShortLiteralCount() {
		return shortened;
	}
	
	public AssemblyDocument getRootDocument() {
		return rootDocument;
	}
	
	public IFile getFile() {
		return rootDocument.getFile();
	}

	public boolean isLabelsCaseSensitive() {
		return labelsCaseSensitive;
	}

	public static void setLabelsCaseSensitive(boolean labelsCaseSensitive) {
		Assembly.labelsCaseSensitive = labelsCaseSensitive;
	}

	private int timerEnd() {
		return (int) ((System.nanoTime() - timer) / 1e6f);
	}
	
	private int timerReset() {
		long end = System.nanoTime();
		int delta = (int) ((end - timer) / 1e6f);
		timer = end;
		return delta;
	}

	private void timerStart() {
		this.timer = System.nanoTime();
	}
}