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

export interface RendererState {
  mute: boolean
  volume: number
  playback: number
  name: string
  uri: string
  metadata: string
  position: string
  duration: string
  buffer: number
}

export interface Renderer {
  id: number
  name: string
  address: string
  uuid: string
  icon: string
  playing: string
  time: string
  progressPercent: number
  isActive: boolean
  isAllowed: boolean
  isAuthenticated: boolean
  userId: number
  controls: number
  state: RendererState
}

export interface NetworkDevice {
  hostName: string
  ipAddress: string
  lastSeen: number
}

export interface NetworkDevicesFilter {
  name: string
  isAllowed: boolean
  isDefault: boolean
  devices: NetworkDevice[]
}

export interface User {
  value: number
  label: string
}

export interface Media {
  value: string
  label: string
  browsable: boolean
}
