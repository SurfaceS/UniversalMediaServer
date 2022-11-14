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
package net.pms.parsers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import net.pms.dlna.InputFile;
import net.pms.formats.Format;
import net.pms.media.Media;
import net.pms.renderers.Renderer;
import net.pms.util.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaParser.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private MediaParser() {
	}

	/**
	 * Chooses which parsing method to parse the file with.
	 */
	public static void parse(Media media, InputFile file, Format ext, int type, Renderer renderer) {
		if (file.getFile() != null) {
			if (ext.getIdentifier() == Format.Identifier.RA) {
				// Special parsing for RealAudio 1.0 and 2.0 which isn't handled by MediaInfo or JAudioTagger
				FileChannel channel;
				try {
					channel = FileChannel.open(file.getFile().toPath(), StandardOpenOption.READ);
					if (AudioUtils.parseRealAudio(channel, media)) {
						// If successful parsing is done, if not continue parsing the standard way
						media.postParse(type, file);
						return;
					}
				} catch (IOException e) {
					LOGGER.warn("An error occurred when trying to open \"{}\" for reading: {}", file, e.getMessage());
					LOGGER.trace("", e);
				}
			}
			if (ext.getIdentifier() == Format.Identifier.RAW) {
				RawImageParser.parse(media, file, type);
				return;
			}

			// MediaInfo can't correctly parse ADPCM, DFF, DSF or PNM
			if (
				renderer.isUseMediaInfo() &&
				ext.getIdentifier() != Format.Identifier.ADPCM &&
				ext.getIdentifier() != Format.Identifier.DFF &&
				ext.getIdentifier() != Format.Identifier.DSF &&
				ext.getIdentifier() != Format.Identifier.PNM
			) {
				MediaInfoParser.parse(media, file, type);
			} else {
				media.parse(file, ext, type, false, false);
			}
		} else {
			media.parse(file, ext, type, false, false);
		}
	}

}
