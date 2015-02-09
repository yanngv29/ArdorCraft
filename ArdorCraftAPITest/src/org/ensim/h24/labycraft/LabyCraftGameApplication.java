package org.ensim.h24.labycraft;

import com.ardorcraft.base.ArdorBaseApplication;

public class LabyCraftGameApplication extends ArdorBaseApplication {

	public LabyCraftGameApplication() {
		super(new LabyCraftGame());
	}

	public static void main(final String[] args) {
		final ArdorBaseApplication example = new LabyCraftGameApplication();
		new Thread(example, "MainArdorThread").start();
	}
}
