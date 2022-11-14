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

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.InputFile;
import net.pms.encoders.DCRaw;
import net.pms.encoders.EngineFactory;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.media.Media;
import net.pms.util.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawImageParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(RawImageParser.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private RawImageParser() {
	}

	/**
	 * Parses media info from FFmpeg's stderr output
	 *
	 * @param lines The stderr output
	 * @param input The FFmpeg input (-i) argument used
	 */
	public static void parse(Media media, InputFile file, int type) {
		try {
			// Only parse using DCRaw if it is enabled
			DCRaw dcraw = (DCRaw) EngineFactory.getActiveEngine(DCRaw.ID);
			if (dcraw != null) {
				LOGGER.trace("Parsing RAW image \"{}\" with DCRaw", file.getFile().getName());
				dcraw.parse(media, file.getFile());

				media.setContainer(FormatConfiguration.RAW);

				ImageInfo imageInfo = null;
				Metadata metadata;
				FileType fileType = FileType.Unknown;
				try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(file.getFile().toPath()))) {
					fileType = FileTypeDetector.detectFileType(inputStream);
					metadata = ImagesUtil.getMetadata(inputStream, fileType);
				} catch (IOException e) {
					metadata = new Metadata();
					LOGGER.debug("Error reading \"{}\": {}", file.getFile().getAbsolutePath(), e.getMessage());
					LOGGER.trace("", e);
				} catch (ImageProcessingException e) {
					metadata = new Metadata();
					LOGGER.debug(
						"Error parsing {} metadata for \"{}\": {}",
						fileType.toString().toUpperCase(Locale.ROOT),
						file.getFile().getAbsolutePath(),
						e.getMessage()
					);
					LOGGER.trace("", e);
				}
				if (fileType == FileType.Arw && !ImagesUtil.isARW(metadata)) {
					fileType = FileType.Tiff;
				}
				ImageFormat format = ImageFormat.toImageFormat(fileType);
				if (format == null || format == ImageFormat.TIFF) {
					format = ImageFormat.toImageFormat(metadata);
					if (format == null || format == ImageFormat.TIFF) {
						format = ImageFormat.RAW;
					}
				}
				try {
					imageInfo = ImageInfo.create(
						media.getWidth(),
						media.getHeight(),
						metadata,
						format,
						file.getSize(),
						true,
						false
					);
					LOGGER.trace("Parsing of RAW image \"{}\" completed: {}", file.getFile().getName(), imageInfo);
				} catch (ParseException e) {
					LOGGER.warn("Unable to parse \"{}\": {}", file.getFile().getAbsolutePath(), e.getMessage());
					LOGGER.trace("", e);
				}

				media.setImageInfo(imageInfo);

				if (media.getWidth() > 0 && media.getHeight() > 0 && PMS.getConfiguration().getImageThumbnailsEnabled()) {
					byte[] image = dcraw.getThumbnail(null, file.getFile().getAbsolutePath(), imageInfo);
					media.setThumb(DLNAThumbnail.toThumbnail(image, 320, 320, ImagesUtil.ScaleType.MAX, ImageFormat.JPEG, false));
				}
			} else {
				LOGGER.trace(
					"Parsing RAW image \"{}\" as a regular image because DCRaw is disabled",
					file.getFile().getName()
				);
				ImagesUtil.parseImage(file.getFile(), media);
			}
			media.setSize(file.getSize());
			media.setImageCount(1);
			media.postParse(type, file);
			media.setMediaparsed(true);
		} catch (IOException e) {
			LOGGER.error(
				"Error parsing RAW file \"{}\": {}",
				file.getFile().getAbsolutePath(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}
}
