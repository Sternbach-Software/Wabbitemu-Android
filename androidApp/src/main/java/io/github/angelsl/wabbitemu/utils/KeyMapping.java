package io.github.angelsl.wabbitemu.utils;

public class KeyMapping {
	int key;
	int group;
	int bit;

	public KeyMapping(final int key, final int group, final int bit) {
		this.key = key;
		this.group = group;
		this.bit = bit;
	}

	public int getKey() {
		return key;
	}

	public int getGroup() {
		return group;
	}

	public int getBit() {
		return bit;
	}
}
