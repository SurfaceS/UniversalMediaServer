/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network;

import net.pms.formats.Format;
import net.pms.service.Services;
import net.pms.store.StoreItem;

// a utility class, instances of which trigger start/stop callbacks before/after streaming a item
public class StartStopListener {

	private final String rendererId;
	private final StoreItem item;
	private boolean started = false;
	private boolean stopped = false;

	public StartStopListener(String rendererId, StoreItem item) {
		this.rendererId = rendererId;
		this.item = item;
	}

	// technically, these don't need to be synchronized as there should be
	// one thread per request/response, but it doesn't hurt to enforce the contract
	public synchronized void start() {
		assert this.item != null;
		Format format = item.getFormat();
		// only trigger the start/stop events for audio and video
		if (!started && format != null && (format.isVideo() || format.isAudio())) {
			item.startPlaying(rendererId);
			started = true;
			Services.sleepManager().startPlaying();
		} else {
			Services.postponeSleep();
		}
	}

	public synchronized void setBytesSent(long bytesSent) {
		//here we have access to the StoreItem, and can follow the bytes sent.
		//we may use this to store bytes sent from the same range on previous
		//request, or something like that to start only if we are sure it need.
		//For now, I put a start if more than 100k was received by renderer.
		if (!started && bytesSent > 100000) {
			start();
		}
	}

	public synchronized void stop() {
		if (started && !stopped) {
			item.stopPlaying(rendererId);
			stopped = true;
			Services.sleepManager().stopPlaying();
		}
	}

}
