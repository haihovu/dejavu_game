/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.game;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Represents an animated character.
 * @author hai
 */
public class DvCharacter {
	/**
	 * The states of the characters.
	 */
	public static enum State {
		MOVING,
		STOPPED
	}
	private final Image imageStatic;
	private final Image[] imageMoving;
	private Point point = new Point(0, 0);
	private double scale = 1.0;
	public final String name;
	private int imgIdx;
	private long lastUpdate;
	private State state = State.STOPPED;
	private final Dimension dimension = new Dimension();
	/**
	 * Creates a new game character.
	 * @param name The name of the character
	 * @param staticImg A single image representing the character at rest.
	 * @param movingImg A series of images representing the character in motion.
	 */
	public DvCharacter(String name, Image staticImg, Image[] movingImg) {
		this.name = name;
		this.imageStatic = staticImg;
		this.imageMoving = movingImg;
		dimension.width = staticImg.getWidth(null);
		dimension.height = staticImg.getHeight(null);
	}

	/**
	 * Specifies the current state of the character. May be invoked from any thread.
	 * @param state The new state
	 * @return This object.
	 */
	public DvCharacter setState(State state) {
		synchronized(this) {
			this.state = state;
		}
		return this;
	}
	/**
	 * Retrieves the current state of this character. May be invoked from any thread.
	 * @return The current state of this character.
	 */
	public State getState() {
		synchronized(this) {
			return state;
		}
	}
	
	/**
	 * Specifies the scaling factor for this character. May be invoked from
	 * any thread.
	 * @param scale The new value.
	 * @return This object.
	 */
	public DvCharacter setScale(double scale) {
		synchronized (this) {
			this.scale = scale;
			dimension.width = (int)(imageStatic.getWidth(null) * scale);
			dimension.height = (int)(imageStatic.getHeight(null) * scale);
		}
		return this;
	}

	/**
	 * Retrieves the dimension of this character. May be invoked from any thread.
	 * @return A copy of the dimension of this character.
	 */
	public Dimension getDimension() {
		synchronized(this) {
			return new Dimension(dimension);
		}
	}
	
	/**
	 * Retrieves the scale factor for this character. May be invoked from any thread.
	 * @return The scale factor for this character.
	 */
	public double getScale() {
		synchronized(this) {
			return scale;
		}
	}
	/**
	 * Retrieves the bounds for this character. May be invoked from any thread.
	 * @return The current bounds, not null.
	 */
	public Rectangle getBounds() {
		Point pt = getPoint();
		Dimension dim = getDimension();
		return new Rectangle(pt.x, pt.y, dim.width, dim.height);
	}
	/**
	 * Fetches the next image to be displayed for this character.
	 * May be invoked from any thread.
	 * @return The next image. Not null.
	 */
	public Image getNextImage() {
		synchronized(this) {
			if(getState() == State.MOVING) {
				long ts = System.currentTimeMillis();
				if(imgIdx < imageMoving.length) {
					if((ts - lastUpdate) > 200) {
						lastUpdate = ts;
						++imgIdx;
						if(imgIdx >= imageMoving.length) {
							imgIdx = 0;
						}
					}
				}
				return imageMoving[imgIdx];
			}
		}
		return imageStatic;
	}
	
	/**
	 * Specifies the current point (position) of this character.
	 * May be invoked from any thread.
	 * @param pnt This character's new current point.
	 * @return This character.
	 */
	public DvCharacter setPoint(Point pnt) {
		synchronized (this) {
			this.point = pnt;
		}
		return this;
	}

	/**
	 * Retrieves a COPY of the current point (position) of this character.
	 * May be invoked from any thread.
	 * @return The copy of this character's current point.
	 */
	public Point getPoint() {
		synchronized (this) {
			return new Point(point);
		}
	}
}
