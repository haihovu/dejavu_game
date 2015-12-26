/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.game;

import java.awt.Image;

/**
 * The abstract class for all targets.
 * @author hai
 */
public abstract class DvTarget extends DvCharacter {
	/**
	 * Listener interface for handling targets' events.
	 */
	public interface TargetListener {
		/**
		 * A target was hit
		 * @param name Name of the target.
		 */
		void hit(String name);
		/**
		 * A target was destroyed.
		 * @param name Name of the target.
		 */
		void destroyed(String name);
	}
	/**
	 * To be used by sub-classes.
	 * @param name Name of the target.
	 * @param stillImg Image representing the still target.
	 * @param movingImg Images representing the moving target.
	 */
	protected DvTarget(String name, Image stillImg, Image[] movingImg) {
		super(name, stillImg, movingImg);
	}
	/**
	 * Target was destroyed. Sub-classes must implement the behaviour of a destroyed object.
	 */
	public abstract void destroyed();
	/**
	 * Target was hit. Sub-classes must implement the behaviour of a hit object.
	 */
	public abstract void hit();
}
