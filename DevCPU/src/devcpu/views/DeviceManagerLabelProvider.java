package devcpu.views;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import devcpu.Activator;
import devcpu.emulation.DefaultControllableDCPU;
import devcpu.emulation.FloppyDisk;
import devcpu.emulation.Ship;
import devcpu.emulation.VirtualClock;
import devcpu.emulation.VirtualFloppyDrive;
import devcpu.emulation.VirtualKeyboard;
import devcpu.emulation.VirtualMonitor;
import devcpu.emulation.VirtualSleepChamber;
import devcpu.emulation.VirtualVectorDisplay;
import devcpu.managers.DCPUManager;
import devcpu.managers.FloppyManager;
import devcpu.managers.HardwareManager;

public class DeviceManagerLabelProvider extends LabelProvider {
	@Override
	public String getText(Object o) {
		if (o instanceof Ship) {
			return ((Ship)o).getName();
		} else if (o instanceof DCPUManager) {
			return "DCPUs";
		} else if (o instanceof HardwareManager) {
			return "Hardware";
		} else if (o instanceof FloppyManager) {
			return "Floppies";
		} else if (o instanceof DefaultControllableDCPU) {
			return ((DefaultControllableDCPU)o).getID();
		} else if (o instanceof VirtualClock) {
			return ((VirtualClock)o).getID();
		} else if (o instanceof VirtualFloppyDrive) {
			return ((VirtualFloppyDrive)o).getID();
		} else if (o instanceof VirtualKeyboard) {
			return ((VirtualKeyboard)o).getID();
		} else if (o instanceof VirtualSleepChamber) {
			return ((VirtualSleepChamber)o).getID();
		} else if (o instanceof VirtualVectorDisplay) {
			return ((VirtualVectorDisplay)o).getID();
		} else if (o instanceof VirtualMonitor) {
			return ((VirtualMonitor)o).getID();
		} else if (o instanceof FloppyDisk) {
			return ((FloppyDisk)o).getID();
		} else {
			return "Unknown";
		}
	}
	
	@Override
	public Image getImage(Object o) {
		if (o instanceof DefaultControllableDCPU)
		{
			return new Image(Display.getCurrent(), loadResource("icons/dcpu.png"));
		} else if (o instanceof VirtualMonitor)
		{
			return new Image(Display.getCurrent(), loadResource("icons/lem.png"));
		} else if (o instanceof VirtualKeyboard)
		{
			return new Image(Display.getCurrent(), loadResource("icons/keyboard.png"));
		} else if (o instanceof VirtualVectorDisplay)
		{
			return new Image(Display.getCurrent(), loadResource("icons/sped.png"));
		} else if (o instanceof VirtualSleepChamber)
		{
			return new Image(Display.getCurrent(), loadResource("icons/spc.png"));
		} else if (o instanceof VirtualFloppyDrive)
		{
			return new Image(Display.getCurrent(), loadResource("icons/fd.png"));
		} else if (o instanceof VirtualClock)
		{
			return new Image(Display.getCurrent(), loadResource("icons/clock.png"));
		} else if (o instanceof FloppyDisk)
		{
			if (((FloppyDisk) o).isWriteProtected()) {
				return new Image(Display.getCurrent(), loadResource("icons/protecteddisk.png"));
			} else {
				return new Image(Display.getCurrent(), loadResource("icons/disk.png"));
			}
		} else if (o instanceof FloppyManager)
		{
			return new Image(Display.getCurrent(), loadResource("icons/disk.png"));
		} else if (o instanceof DCPUManager)
		{
			return new Image(Display.getCurrent(), loadResource("icons/dcpu.png"));
		} else if (o instanceof HardwareManager)
		{
			return new Image(Display.getCurrent(), loadResource("icons/hw.png"));
		} else if (o instanceof Ship)
		{
			return new Image(Display.getCurrent(), loadResource("icons/ship.png"));
		}
		return super.getImage(o);
	}

	private String loadResource(String filename) {
		URL url = FileLocator.find(Activator.getDefault().getBundle(), new org.eclipse.core.runtime.Path(filename), Collections.EMPTY_MAP);
		URL fileUrl = null;
		try {
		fileUrl = FileLocator.toFileURL(url);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			return fileUrl.toURI().getRawPath();
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
	
}
