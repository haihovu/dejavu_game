/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.game;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.dejavu.guiutil.DjvGuiUtil;
import org.dejavu.util.DjvBackgroundTask;

/**
 * Representing a single game character that can be controlled. This is a higher
 * abstraction than the DvCharacter concept which represents just the appearance
 * of the character. The actor concept involves connecting the character to an
 * animated panel, and the movement of said character using the keyboard control
 * concept (DvControlKey).
 */
public class DvActor {
	/**
	 * Handle a single control key, i.e. key for moving the game character in a certain direction
	 */
	private class KeyHandler extends AbstractAction {
		private final DvControlKey.Direction direction;
		/**
		 * Creates a new control key handler
		 * @param dir The direction associated with this type of key events.
		 */
		private KeyHandler(DvControlKey.Direction dir) {
			direction = dir;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			character.setState(DvCharacter.State.MOVING);
			switch(direction) {
				case DOWN:
					move(new Point(0, increment));
					break;
				case UP:
					move(new Point(0, -increment));
					break;
				case RIGHT:
					move(new Point(increment, 0));
					break;
				case LEFT:
					move(new Point(-increment, 0));
					break;
			}
			synchronized(DvActor.this) {
				lastMoved = System.currentTimeMillis();
			}
		}
	}
	/**
	 * Background task for detecting when the actor has stopped.
	 * This changes the state of the character if it is deemed to have stopped
	 * moving.
	 */
	private class StopDetection extends DjvBackgroundTask {
		/**
		 * Creates a new stop detector.
		 */
		private StopDetection() {
			super("StopDetection");
		}

		@Override
		public void run() {
			try {
				while(getRunFlag()) {
					long ts = System.currentTimeMillis();
					synchronized(DvActor.this) {
						if(lastMoved != 0) {
							// If the character has not moved in 100ms then we consider him stopped.
							if((ts - lastMoved) > 100) {
								lastMoved = 0;
								character.setState(DvCharacter.State.STOPPED);
								move(new Point());
							}
						}
						
						// Run this loop every 500ms
						DvActor.this.wait(500);
					}
				}
			} catch(InterruptedException e) {
			} finally {
				synchronized(DvActor.this) {
					if(stopDetection == this) {
						stopDetection = null;
					}
				}
			}
		}
	}
	
	private DjvBackgroundTask stopDetection;
	private long lastMoved;
	private final DvAnimatedPanel animation;
	private static final int increment = 4;
	/**
	 * The visual representation of this actor.
	 */
	public final DvCharacter character;
	/**
	 * Creates a new game actor.
	 * @param character The image for this actor
	 * @param animation The animation panel in which this actor 'lives'
	 */
	public DvActor(DvCharacter character, DvAnimatedPanel animation) {
		this.character = character;
		this.animation = animation;
	}
	/**
	 * Connects the actor to an animated panel, via keyboard events.
	 * @param animatedPanel The animated panel against which the actor is connected.
	 * @param controlKeys The control keys with which to connect the component to the actor.
	 * @return This actor.
	 */
	public DvActor connectToComponent(DvAnimatedPanel animatedPanel, DvControlKey[] controlKeys) {
		for(DvControlKey key : controlKeys) {
			DjvGuiUtil.registerKeyAction(animatedPanel.getRootPane(), key.keyEvent, new KeyHandler(key.direction));
		}
		synchronized(this) {
			if(stopDetection != null) {
				stopDetection.stop();
			}
			stopDetection = new StopDetection().start();
		}
		return this;
	}
	/**
	 * Disconnect this actor.
	 */
	public void disconnect() {
		synchronized(this) {
			if(stopDetection != null) {
				stopDetection.stop();
			}
		}
	}
	
	/**
	 * Moves the actor by a delta amount in the animation pane. This simply moves
	 * the point's (location) attribute by a specific amount. The animation panel
	 * will display the actor in the new location at its next frame.
	 * Must be invoked from the EDT.
	 * @param delta The delta amount to move the actor.
	 * @return This actor.
	 */
	public DvActor move(Point delta) {
		if((delta.x != 0)||(delta.y != 0)) {
			Rectangle bounds = animation.getBounds();
			Point location = character.getPoint();
			Dimension dim = character.getDimension();
			int xlim = bounds.width - dim.width;
			int ylim = bounds.height - dim.height;
			location.x += delta.x;
			location.y += delta.y;
			if(location.x < 0) {
				location.x = 0;
			} else if(location.x > xlim) {
				location.x = xlim;
			}
			if(location.y < 0) {
				location.y = 0;
			} else if(location.y > ylim) {
				location.y = ylim;
			}
			character.setPoint(location);
		}
		
		return this;
	}
}
