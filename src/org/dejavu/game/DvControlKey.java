/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.game;

/**
 * A single control key binding (UP/DOWN/LEFT/RIGHT), i.e. binding a key press
 * with the movement of the game character.
 */
public class DvControlKey {
	/**
	 * All supported directions
	 */
	public static enum Direction {
		UP,
		DOWN,
		LEFT,
		RIGHT
	}
	public final Direction direction;
	public final int keyEvent;
	/**
	 * Creates a new control key.
	 * @param dir The direction represented by the key.
	 * @param key The value of the key as defined by the VK values in class KeyEvent.
	 */
	public DvControlKey(Direction dir, int key) {
		this.direction = dir;
		this.keyEvent = key;
	}
}
