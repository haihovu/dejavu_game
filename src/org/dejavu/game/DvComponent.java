/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.game;

import java.awt.Graphics2D;

/**
 * Base class for all components that know how to render themselves.
 * @author hai
 */
public abstract class DvComponent {
	private final String name;
	private double scale = 1.0;
	/**
	 * Meant to be used only by sub-classes.
	 * @param name The name of the component.
	 */
	protected DvComponent(String name) {
		name.getClass(); // Null check.
		this.name = name;
	}
	/**
	 * Retrieves the name of the component.
	 * @return The component's name, not null.
	 */
	public String getName() {
		return name;
	}
	/**
	 * Retrieves the scaling factor for this component.
	 * @return The scaling factor.
	 */
	public double getScale() {
		synchronized(this) {
			return scale;
		}
	}
	/**
	 * Specifies the scaling factor for this component.
	 * @param scale The new scaling factor.
	 * @return This object.
	 */
	public DvComponent setScale(double scale) {
		synchronized(this) {
			this.scale = scale;
		}
		return this;
	}
	/**
	 * All sub-classes must implement this.
	 * @param g2d The Graphics 2D context with which to draw the component.
	 */
	public abstract void draw(Graphics2D g2d);
}
