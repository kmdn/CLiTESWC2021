package install;

import structure.config.kg.EnumModelType;

public class InstallFiletree {

	public static void main(String[] args) {
		System.out.print("Building file tree...");
		// Makes a simple call to the enum values to initiate the classloader to do its
		// dependency-magic
		EnumModelType.DEFAULT.values();
		System.out.print("Done!");
	}

}
