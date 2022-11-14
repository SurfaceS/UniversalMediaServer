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
package net.pms.dlna;

import net.pms.media.Media;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.sharedcontent.SharedContentArray;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.parsers.MediaInfoParser;
import net.pms.service.Services;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaTest {
	private static final Class<?> CLASS = MediaTest.class;

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTest.class.getName());

	// Look for more formats at https://samples.ffmpeg.org/
	private static final String[] test_files = {
		"video-h264-aac.mp4",
		"video-mpeg4-aac.mkv",
		"video-h265-aac.mkv",
		"video-theora-vorbis.ogg",
		"video-h264-aac.m4v",
		"video-mp4-aac.3g2",
		"video-mp4-adpcm.avi",
		"video-mp4-aac.mov",
		"video-wmv-wma.wmv",
		"video-vp8-vorbis.webm",
		"video-sor-aac.flv",
		"video-h264-aac.avi",
		"audio-lpcm.wav",
		"audio-vorbis.oga",
		"audio-mp3.mp3",
		"video-av1-aac.mp4",
		"video-av1.mp4",
		"video-vc1.mkv",
		"video-h264-heaac.mp4",
		"video-h264-eac3.mkv",
		"audio-flac24.flac",
		"video-xvid-mp3.avi",
		"video-h265_dolbyvision_p05.05-eac3_atmos.mkv",
		"video-h265_dolbyvision_p08.05-eac3_atmos.mkv",
	};

	/**
	 * Set up testing conditions before running the tests.
	 *
	 * @throws ConfigurationException
	 */
	@BeforeAll
	public static final void setUp() throws ConfigurationException, InterruptedException {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
		PMS.configureJNA();
		PMS.forceHeadless();
		try {
			PMS.setConfiguration(new UmsConfiguration(false));
		} catch (Exception ex) {
			throw new AssertionError(ex);
		}
		assert PMS.getConfiguration() != null;
		PMS.getConfiguration().setAutomaticMaximumBitrate(false); // do not test the network speed.
		SharedContentConfiguration.updateSharedContent(new SharedContentArray(), false);
		PMS.getConfiguration().setScanSharedFoldersOnStartup(false);
		PMS.getConfiguration().setUseCache(false);

		Services.destroy();

		try {
			PMS.getConfiguration().initCred();
		} catch (Exception ex) {
			LOGGER.warn("Failed to write credentials configuration", ex);
		}

		// Create a new PMS instance
		PMS.getNewInstance();
	}

	@Test
	public void testContainerProperties() throws Exception {
		// Check if the MediaInfo library is properly installed and initialized
		// especially on Linux which needs users to be involved.
		assertTrue(MediaInfoParser.isValid() ,
			"\r\nYou do not appear to have MediaInfo installed on your machine, please install it before running this test\r\n");

		// Create handles to the test content
		// This comes from RequestV2::answer()
		DLNAResource parent = new VirtualFolder("test", "test");
		parent.setDefaultRenderer(RendererConfigurations.getDefaultRenderer());
		PMS.getGlobalRepo().add(parent);

		for (int i = 0; i < test_files.length; ++i) {
			DLNAResource dlna = new RealFile(FileUtils.toFile(CLASS.getResource(test_files[i])));
			dlna.setParent(parent);
			dlna.resolveFormat();
			dlna.syncResolve();

			Media mediaInfo = dlna.getMedia();

			switch(i) {
				case 0 -> assertEquals(mediaInfo.toString(),
						"Container: MP4, Size: 1325017, Overall Bitrate: 676979, Duration: 0:00:15.658, Video Tracks: 1 [ Codec: h264, Codec Level: 3, Codec Profile: high, Resolution: 640 x 360, Display Aspect Ratio: 16:9, Scan Type: Progressive, Bit Depth: 8, Frame Rate Mode: CFR (CFR), Reference Frame Count: 4, Matrix Coefficients: BT.601], Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 125547, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/mp4"
					);
				case 1 -> assertEquals(mediaInfo.toString(),
						"Container: MKV, Size: 2097841, Overall Bitrate: 1575843, Duration: 0:00:10.650, Video Tracks: 1 [ Codec: mp4, Codec Level: 1, Codec Profile: simple, Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Scan Type: Progressive, Bit Depth: 8, Frame Rate Mode: VFR (VFR)], Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 0, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska"
					);
				case 2 -> assertEquals(mediaInfo.toString(),
						"Container: MKV, Size: 5291494, Overall Bitrate: 2619551, Duration: 0:00:16.160, Video Tracks: 1 [ Codec: h265, Codec Level: 4@main, Codec Profile: main, Resolution: 1920 x 960, Display Aspect Ratio: 2.00:1, Bit Depth: 8, Frame Rate Mode: CFR (CFR), Matrix Coefficients: BT.709], Audio Tracks: 1 [Id: 0, Language Code: eng, Audio Track Title From Metadata: Stereo, Audio Codec: AAC-LC, Bitrate: 0, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska"
					);
				case 3 -> assertEquals(mediaInfo.toString(),
						"Container: OGG, Size: 1734919, Overall Bitrate: 454643, Duration: 0:00:30.528, Video Tracks: 1 [ Codec: theora, Resolution: 480 x 270, Display Aspect Ratio: 16:9, Video Depth: 0, Bit Depth: 0], Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 112000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/ogg"
					);
				case 4 -> assertEquals(mediaInfo.toString(),
						"Container: MP4, Size: 3538130, Overall Bitrate: 542149, Duration: 0:00:52.209, Video Tracks: 1 [ Codec: h264, Codec Level: 3, Codec Profile: high, Resolution: 720 x 480, Display Aspect Ratio: 2.35:1, Pixel Aspect Ratio: 1.563, Scan Type: Progressive, Bit Depth: 8, Frame Rate Mode: CFR (CFR), Reference Frame Count: 4], Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 128290, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/mp4"
					);
				case 5 -> assertEquals(mediaInfo.toString(),
						"Container: 3G2, Size: 1792091, Overall Bitrate: 275410, Duration: 0:00:52.056, Video Tracks: 1 [ Codec: mp4, Codec Level: 1, Codec Profile: simple, Resolution: 360 x 240, Display Aspect Ratio: 3:2, Scan Type: Progressive, Bit Depth: 8, Frame Rate Mode: CFR (CFR)], Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 59721, Channels: 2, Sample Frequency: 22050 Hz], Mime Type: video/3gpp2"
					);
				case 6 -> assertEquals(mediaInfo.toString(),
						"Container: AVI, Size: 3893340, Overall Bitrate: 598711, Duration: 0:00:52.023, Video Tracks: 1 [Video Track Title: Sintel Trailer,  Codec: mp4, Resolution: 360 x 240, Display Aspect Ratio: 3:2, Video Depth: 0, Bit Depth: 0], Audio Tracks: 1 [Audio Codec: ADPCM, Bitrate: 128000, Bits per Sample: 4, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/avi"
					);
				case 7 -> assertEquals(mediaInfo.toString(),
						"Container: MOV, Size: 2658492, Overall Bitrate: 408339, Duration: 0:00:52.084, Video Tracks: 1 [Video Track Title: Sintel Trailer,  Codec: mp4, Codec Level: 1, Codec Profile: simple, Resolution: 360 x 240, Display Aspect Ratio: 3:2, Scan Type: Progressive, Bit Depth: 8, Frame Rate Mode: CFR (CFR)], Audio Tracks: 1 [Id: 0, Language Code: eng, Audio Codec: AAC-LC, Bitrate: 125805, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/quicktime"
					);
				case 8 -> assertEquals(mediaInfo.toString(),
						"Container: WMV, Size: 3002945, Overall Bitrate: 460107, Duration: 0:00:52.213, Video Tracks: 1 [ Codec: wmv, Resolution: 360 x 240, Display Aspect Ratio: 2.35:1, Pixel Aspect Ratio: 1.563, Bit Depth: 8], Audio Tracks: 1 [Audio Codec: WMA, Bitrate: 128000, Channels: 2, Sample Frequency: 44100 Hz], Mime Type: video/x-ms-wmv"
					);
				case 9 -> assertEquals(mediaInfo.toString(),
						"Container: WEBM, Size: 901185, Overall Bitrate: 236044, Duration: 0:00:30.543, Video Tracks: 1 [ Codec: vp8, Resolution: 480 x 270, Display Aspect Ratio: 16:9, Video Depth: 0, Bit Depth: 0, Frame Rate Mode: CFR (CFR)], Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 112000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/webm"
					);
				case 10 -> assertEquals(mediaInfo.toString(),
						"Container: FLV, Size: 2097492, Overall Bitrate: 1529899, Duration: 0:00:10.968, Video Tracks: 1 [ Codec: sor, Resolution: 1280 x 720, Display Aspect Ratio: 16:9, Bit Depth: 8], Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 375000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/x-flv"
					);
				case 11 -> assertEquals(mediaInfo.toString(),
						"Container: AVI, Size: 742478, Overall Bitrate: 194029, Duration: 0:00:30.613, Video Tracks: 1 [ Codec: h264, Codec Level: 2.1, Codec Profile: high, Resolution: 480 x 270, Display Aspect Ratio: 16:9, Scan Type: Progressive, Bit Depth: 8, Frame Rate Mode: VFR (VFR), Reference Frame Count: 4], Audio Tracks: 1 [Audio Codec: AAC-LC, Bitrate: 139632, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi"
					);
				case 12 -> assertEquals(mediaInfo.toString(),
						"Container: WAV, Size: 1073218, Overall Bitrate: 256062, Bitrate: 256062, Duration: 0:00:33.530, Audio Tracks: 1 [Audio Codec: LPCM, Bitrate: 256000, Channels: 2, Sample Frequency: 8000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/wav"
					);
				case 13 -> assertEquals(mediaInfo.toString(),
						"Container: OGA, Size: 1089524, Overall Bitrate: 117233, Bitrate: 117233, Duration: 0:01:14.349, Audio Tracks: 1 [Audio Codec: Vorbis, Bitrate: 120000, Channels: 2, Sample Frequency: 32000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/ogg"
					);
				case 14 -> assertEquals(mediaInfo.toString(),
						"Container: MP3, Size: 764176, Overall Bitrate: 224000, Bitrate: 224000, Duration: 0:00:27.252, Audio Tracks: 1 [Audio Codec: MP3, Bitrate: 224000, Channels: 2, Sample Frequency: 32000 Hz, Artist: Kevin MacLeod, Album: YouTube Audio Library, Track Name: Impact Moderato, Genre: Cinematic], Mime Type: audio/mpeg"
					);
				case 15 -> assertEquals(mediaInfo.toString(),
						"Container: MP4, Size: 245747, Overall Bitrate: 130716, Duration: 0:00:15.040, Video Tracks: 1 [Video Track Title: vid,  Codec: av1, Codec Level: 3.0, Codec Profile: main, Resolution: 960 x 540, Display Aspect Ratio: 16:9, Bit Depth: 8, Frame Rate Mode: CFR (CFR)], Audio Tracks: 1 [Id: 0, Language Code: snd, Audio Track Title From Metadata: snd, Audio Codec: AAC-LC, Bitrate: 8887, Channel: 1, Sample Frequency: 32000 Hz], Mime Type: video/mp4"
					);
				case 16 -> assertEquals(mediaInfo.toString(),
						"Container: MP4, Size: 690235, Overall Bitrate: 952377, Duration: 0:00:05.798, Video Tracks: 1 [Video Track Title: ivf@GPAC0.7.2-DEV-rev654-gb6f7409ce-github_master,  Codec: av1, Codec Level: 2.0, Codec Profile: main, Resolution: 480 x 270, Display Aspect Ratio: 16:9, Bit Depth: 8, Frame Rate Mode: CFR (CFR)], Mime Type: video/mp4"
					);
				case 17 -> assertEquals(mediaInfo.toString(),
						"Container: MKV, Size: 6291087, Overall Bitrate: 22468168, Duration: 0:00:02.240, Video Tracks: 1 [ Codec: vc1, Codec Level: 3, Codec Profile: advanced, Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Scan Type: Interlaced, Scan Order: Top Field First, Bit Depth: 8, Frame Rate Mode: CFR (CFR)], Mime Type: video/x-matroska"
					);
				case 18 -> assertEquals(mediaInfo.toString(),
						"Container: MP4, Size: 1099408, Overall Bitrate: 188638, Duration: 0:00:46.625, Video Tracks: 1 [ Codec: h264, Codec Level: 3.1, Codec Profile: main, Resolution: 800 x 600, Display Aspect Ratio: 4:3, Scan Type: Progressive, Bit Depth: 8, Frame Rate Mode: CFR (CFR), Reference Frame Count: 4], Audio Tracks: 1 [Audio Codec: HE-AAC, Bitrate: 159992, Channels: 6, Sample Frequency: 44100 Hz], Mime Type: video/mp4"
					);
				case 19 -> assertEquals(mediaInfo.toString(),
						"Container: MKV, Size: 6356992, Overall Bitrate: 7121, Duration: 1:59:01.690, Video Tracks: 1 [ Codec: h264, Codec Level: 5.1, Codec Profile: main, Resolution: 1280 x 544, Display Aspect Ratio: 2.35:1, Scan Type: Progressive, Bit Depth: 8, Frame Rate Mode: CFR (CFR), Reference Frame Count: 1], Audio Tracks: 1 [Audio Codec: Enhanced AC-3, Bitrate: 1536000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska"
					);
				case 20 -> assertEquals(mediaInfo.toString(),
						"Container: FLAC, Size: 3208022, Overall Bitrate: 1231959, Bitrate: 1231959, Duration: 0:00:20.832, Audio Tracks: 1 [Audio Codec: FLAC, Bitrate: 1231916, Bits per Sample: 24, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: audio/x-flac"
					);
				case 21 -> assertEquals(mediaInfo.toString(),
						"Container: AVI, Size: 1282694, Overall Bitrate: 793255, Duration: 0:00:12.936, Video Tracks: 1 [ Codec: divx, Codec Level: 5, Codec Profile: advanced simple, Resolution: 720 x 400, Display Aspect Ratio: 16:9, Scan Type: Progressive, Bit Depth: 8], Audio Tracks: 1 [Audio Track Title From Metadata: video-mpeg4-aac, Audio Codec: MP3, Bitrate: 128000, Channels: 2, Sample Frequency: 48000 Hz], Mime Type: video/avi"
					);
				case 22 -> assertEquals(mediaInfo.toString(),
						"Container: MKV, Size: 8925360, Overall Bitrate: 11868830, Duration: 0:00:06.016, Video Tracks: 1 [ Codec: h265, Codec Level: 5.1@main, Codec Profile: main 10, Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Video Depth: 10, Bit Depth: 10, Frame Rate Mode: CFR (CFR)], Audio Tracks: 1 [Audio Codec: Enhanced AC-3, Bitrate: 640000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska"
					);
				case 23 -> assertEquals(mediaInfo.toString(),
						"Container: MKV, Size: 7799945, Overall Bitrate: 10372267, Duration: 0:00:06.016, Video Tracks: 1 [ Codec: h265, Codec Level: 5.1@main, Codec Profile: main 10, Resolution: 1920 x 1080, Display Aspect Ratio: 16:9, Video Depth: 10, Bit Depth: 10, Frame Rate Mode: CFR (CFR), Matrix Coefficients: BT.2020 non-constant], Audio Tracks: 1 [Audio Codec: Enhanced AC-3, Bitrate: 640000, Channels: 6, Sample Frequency: 48000 Hz], Mime Type: video/x-matroska"
					);
				default -> {
				}
			}
		}
	}

}
