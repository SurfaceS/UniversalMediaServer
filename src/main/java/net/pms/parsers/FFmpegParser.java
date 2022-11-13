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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.StringTokenizer;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaChapter;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaLang;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAMediaVideo;
import net.pms.formats.v2.SubtitleType;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegParser.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private FFmpegParser() {
	}

	/**
	 * Parses media info from FFmpeg's stderr output
	 *
	 * @param lines The stderr output
	 * @param input The FFmpeg input (-i) argument used
	 */
	public static void parse(DLNAMediaInfo media, List<String> lines, String input) {
		if (lines != null) {
			if ("-".equals(input)) {
				input = "pipe:";
			}

			boolean matches = false;
			int langId = 0;
			int subId = 0;
			ListIterator<String> fFmpegMetaData = lines.listIterator();

			for (String line : lines) {
				fFmpegMetaData.next();
				line = line.trim();
				if (line.startsWith("Output")) {
					matches = false;
				} else if (line.startsWith("Input")) {
					if (line.contains(input)) {
						matches = true;
						String container = line.substring(10, line.indexOf(',', 11)).trim();

						/**
						 * This method is very inaccurate because the Input line in the FFmpeg output
						 * returns "mov,mp4,m4a,3gp,3g2,mj2" for all 6 of those formats, meaning that
						 * we think they are all "mov".
						 *
						 * Here we workaround it by using the file extension, but the best idea is to
						 * prevent using this method by using MediaInfo=true in renderer configs.
						 */
						if ("mov".equals(container)) {
							container = line.substring(line.lastIndexOf('.') + 1, line.lastIndexOf('\'')).trim();
							LOGGER.trace("Setting container to " + container + " from the filename. To prevent false-positives, use MediaInfo=true in the renderer config.");
						}
						media.setContainer(container);
					} else {
						matches = false;
					}
				} else if (matches) {
					if (line.contains("Duration")) {
						StringTokenizer st = new StringTokenizer(line, ",");
						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							if (token.startsWith("Duration: ")) {
								String durationStr = token.substring(10);
								int l = durationStr.substring(durationStr.indexOf('.') + 1).length();
								if (l < 4) {
									durationStr += "00".substring(0, 3 - l);
								}
								if (durationStr.contains("N/A")) {
									media.setDuration(null);
								} else {
									media.setDuration(parseDurationString(durationStr));
								}
							} else if (token.startsWith("bitrate: ")) {
								String bitr = token.substring(9);
								int spacepos = bitr.indexOf(' ');
								if (spacepos > -1) {
									String value = bitr.substring(0, spacepos);
									String unit = bitr.substring(spacepos + 1);
									int bitrate = Integer.parseInt(value);
									if (unit.equals("kb/s")) {
										bitrate = 1024 * bitrate;
									}
									if (unit.equals("mb/s")) {
										bitrate = 1048576 * bitrate;
									}
									media.setBitrate(bitrate);
								}
							}
						}
					} else if (line.contains("Audio:")) {
						StringTokenizer st = new StringTokenizer(line, ",");
						int a = line.indexOf('(');
						int b = line.indexOf("):", a);
						DLNAMediaAudio audio = new DLNAMediaAudio();
						audio.setId(langId++);
						if (a > -1 && b > a) {
							audio.setLang(line.substring(a + 1, b));
						} else {
							audio.setLang(DLNAMediaLang.UND);
						}

						// Get TS IDs
						a = line.indexOf("[0x");
						b = line.indexOf(']', a);
						if (a > -1 && b > a + 3) {
							String idString = line.substring(a + 3, b);
							try {
								audio.setId(Integer.parseInt(idString, 16));
							} catch (NumberFormatException nfe) {
								LOGGER.debug("Error parsing Stream ID: " + idString);
							}
						}

						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							if (token.startsWith("Stream")) {
								String audioString = "Audio: ";
								int positionAfterAudioString = token.indexOf(audioString) + audioString.length();
								String codec;

								/**
								 * Check whether there are more details after the audio string.
								 * e.g. "Audio: aac (LC)"
								 */
								if (token.indexOf(" ", positionAfterAudioString) != -1) {
									codec = token.substring(positionAfterAudioString, token.indexOf(" ", positionAfterAudioString)).trim();

									// workaround for AAC audio formats
									if (codec.equals("aac")) {
										if (token.contains("(LC)")) {
											codec = FormatConfiguration.AAC_LC;
										} else if (token.contains("(HE-AAC)")) {
											codec = FormatConfiguration.HE_AAC;
										}
									}
								} else {
									codec = token.substring(positionAfterAudioString);

									// workaround for AAC audio formats
									if (codec.equals("aac")) {
										codec = FormatConfiguration.AAC_LC;
									}
								}

								audio.setCodecA(codec);
							} else if (token.endsWith("Hz")) {
								audio.setSampleFrequency(token.substring(0, token.indexOf("Hz")).trim());
							} else if (token.equals("mono")) {
								audio.getAudioProperties().setNumberOfChannels(1);
							} else if (token.equals("stereo")) {
								audio.getAudioProperties().setNumberOfChannels(2);
							} else if (token.equals("5:1") || token.equals("5.1") || token.equals("6 channels")) {
								audio.getAudioProperties().setNumberOfChannels(6);
							} else if (token.equals("5 channels")) {
								audio.getAudioProperties().setNumberOfChannels(5);
							} else if (token.equals("4 channels")) {
								audio.getAudioProperties().setNumberOfChannels(4);
							} else if (token.equals("2 channels")) {
								audio.getAudioProperties().setNumberOfChannels(2);
							} else if (token.equals("s32")) {
								audio.setBitsperSample(32);
							} else if (token.equals("s24")) {
								audio.setBitsperSample(24);
							} else if (token.equals("s16")) {
								audio.setBitsperSample(16);
							}
						}
						int fFmpegMetaDataNr = fFmpegMetaData.nextIndex();

						if (fFmpegMetaDataNr > -1) {
							line = lines.get(fFmpegMetaDataNr);
						}

						if (line.contains("Metadata:")) {
							fFmpegMetaDataNr += 1;
							line = lines.get(fFmpegMetaDataNr);
							while (line.indexOf("      ") == 0) {
								if (line.toLowerCase().contains("title           :")) {
									int aa = line.indexOf(": ");
									int bb = line.length();
									if (aa > -1 && bb > aa) {
										audio.setAudioTrackTitleFromMetadata(line.substring(aa + 2, bb));
										break;
									}
								} else {
									fFmpegMetaDataNr += 1;
									line = lines.get(fFmpegMetaDataNr);
								}
							}
						}

						media.getAudioTracks().add(audio);
					} else if (line.contains("Video:")) {
						StringTokenizer st = new StringTokenizer(line, ",");
						DLNAMediaVideo video = new DLNAMediaVideo();
						while (st.hasMoreTokens()) {
							String token = st.nextToken().trim();
							if (token.startsWith("Stream")) {
								String videoString = "Video: ";
								int positionAfterVideoString = token.indexOf(videoString) + videoString.length();
								String codec;

								// Check whether there are more details after the video string
								if (token.indexOf(" ", positionAfterVideoString) != -1) {
									codec = token.substring(positionAfterVideoString, token.indexOf(" ", positionAfterVideoString)).trim();
								} else {
									codec = token.substring(positionAfterVideoString);
								}

								video.setCodec(codec);
							} else if ((token.contains("tbc") || token.contains("tb(c)"))) {
								// A/V sync issues with newest FFmpeg, due to the new tbr/tbn/tbc outputs
								// Priority to tb(c)
								String frameRateDoubleString = token.substring(0, token.indexOf("tb")).trim();
								try {
									// tbc taken into account only if different than tbr
									if (!frameRateDoubleString.equals(video.getFrameRate())) {
										Double frameRateDouble = Double.valueOf(frameRateDoubleString);
										video.setFrameRate(String.format(Locale.ENGLISH, "%.2f", frameRateDouble / 2));
									}
								} catch (NumberFormatException nfe) {
									// Could happen if tbc is "1k" or something like that, no big deal
									LOGGER.debug("Could not parse frame rate \"" + frameRateDoubleString + "\"");
								}

							} else if ((token.contains("tbr") || token.contains("tb(r)")) && video.getFrameRate() == null) {
								video.setFrameRate(token.substring(0, token.indexOf("tb")).trim());
							} else if ((token.contains("fps") || token.contains("fps(r)")) && video.getFrameRate() == null) { // dvr-ms ?
								video.setFrameRate(token.substring(0, token.indexOf("fps")).trim());
							} else if (token.indexOf('x') > -1 && !token.contains("max")) {
								String resolution = token.trim();
								if (resolution.contains(" [")) {
									resolution = resolution.substring(0, resolution.indexOf(" ["));
								}
								try {
									video.setWidth(Integer.parseInt(resolution.substring(0, resolution.indexOf('x'))));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse width from \"" + resolution.substring(0, resolution.indexOf('x')) + "\"");
								}
								try {
									video.setHeight(Integer.parseInt(resolution.substring(resolution.indexOf('x') + 1)));
								} catch (NumberFormatException nfe) {
									LOGGER.debug("Could not parse height from \"" + resolution.substring(resolution.indexOf('x') + 1) + "\"");
								}
							}
						}
						media.getVideoTracks().add(video);
					} else if (line.contains("Subtitle:")) {
						DLNAMediaSubtitle subtitle = new DLNAMediaSubtitle();
						// $ ffmpeg -codecs | grep "^...S"
						// ..S... = Subtitle codec
						// DES... ass                  ASS (Advanced SSA) subtitle
						// DES... dvb_subtitle         DVB subtitles (decoders: dvbsub ) (encoders: dvbsub )
						// ..S... dvb_teletext         DVB teletext
						// DES... dvd_subtitle         DVD subtitles (decoders: dvdsub ) (encoders: dvdsub )
						// ..S... eia_608              EIA-608 closed captions
						// D.S... hdmv_pgs_subtitle    HDMV Presentation Graphic Stream subtitles (decoders: pgssub )
						// D.S... jacosub              JACOsub subtitle
						// D.S... microdvd             MicroDVD subtitle
						// DES... mov_text             MOV text
						// D.S... mpl2                 MPL2 subtitle
						// D.S... pjs                  PJS (Phoenix Japanimation Society) subtitle
						// D.S... realtext             RealText subtitle
						// D.S... sami                 SAMI subtitle
						// DES... srt                  SubRip subtitle with embedded timing
						// DES... ssa                  SSA (SubStation Alpha) subtitle
						// DES... subrip               SubRip subtitle
						// D.S... subviewer            SubViewer subtitle
						// D.S... subviewer1           SubViewer v1 subtitle
						// D.S... text                 raw UTF-8 text
						// D.S... vplayer              VPlayer subtitle
						// D.S... webvtt               WebVTT subtitle
						// DES... xsub                 XSUB
						if (line.contains("srt") || line.contains("subrip")) {
							subtitle.setType(SubtitleType.SUBRIP);
						} else if (line.contains(" text")) {
							// excludes dvb_teletext, mov_text, realtext
							subtitle.setType(SubtitleType.TEXT);
						} else if (line.contains("microdvd")) {
							subtitle.setType(SubtitleType.MICRODVD);
						} else if (line.contains("sami")) {
							subtitle.setType(SubtitleType.SAMI);
						} else if (line.contains("ass") || line.contains("ssa")) {
							subtitle.setType(SubtitleType.ASS);
						} else if (line.contains("dvd_subtitle")) {
							subtitle.setType(SubtitleType.VOBSUB);
						} else if (line.contains("xsub")) {
							subtitle.setType(SubtitleType.DIVX);
						} else if (line.contains("mov_text")) {
							subtitle.setType(SubtitleType.TX3G);
						} else if (line.contains("webvtt")) {
							subtitle.setType(SubtitleType.WEBVTT);
						} else if (line.contains("eia_608")) {
							subtitle.setType(SubtitleType.EIA608);
						} else if (line.contains("dvb_subtitle")) {
							subtitle.setType(SubtitleType.DVBSUB);
						} else {
							subtitle.setType(SubtitleType.UNKNOWN);
						}
						int a = line.indexOf('(');
						int b = line.indexOf("):", a);
						if (a > -1 && b > a) {
							subtitle.setLang(line.substring(a + 1, b));
						} else {
							subtitle.setLang(DLNAMediaLang.UND);
						}
						subtitle.setId(subId++);
						int fFmpegMetaDataNr = fFmpegMetaData.nextIndex();
						if (fFmpegMetaDataNr > -1) {
							line = lines.get(fFmpegMetaDataNr);
						}
						if (line.contains("Metadata:")) {
							fFmpegMetaDataNr += 1;
							line = lines.get(fFmpegMetaDataNr);
							while (line.indexOf("      ") == 0) {
								if (line.toLowerCase().contains("title           :")) {
									int aa = line.indexOf(": ");
									int bb = line.length();
									if (aa > -1 && bb > aa) {
										subtitle.setSubtitlesTrackTitleFromMetadata(line.substring(aa + 2, bb));
										break;
									}
								} else {
									fFmpegMetaDataNr += 1;
									line = lines.get(fFmpegMetaDataNr);
								}
							}
						}
						media.getSubtitlesTracks().add(subtitle);
					} else if (line.contains("Chapters:")) {
						int fFmpegMetaDataNr = fFmpegMetaData.nextIndex();
						if (fFmpegMetaDataNr > -1) {
							line = lines.get(fFmpegMetaDataNr);
						}
						List<DLNAMediaChapter> ffmpegChapters = new ArrayList<>();
						while (line.contains("Chapter #")) {
							DLNAMediaChapter chapter = new DLNAMediaChapter();
							//set chapter id
							String idStr = line.substring(line.indexOf("Chapter #") + 9);
							if (idStr.contains(" ")) {
								idStr = idStr.substring(0, idStr.indexOf(" "));
							}
							String[] ids = idStr.split(":");
							if (ids.length > 1) {
								chapter.setId(Integer.parseInt(ids[1]));
							} else {
								chapter.setId(Integer.parseInt(ids[0]));
							}
							//set chapter start
							if (line.contains("start ")) {
								String startStr = line.substring(line.indexOf("start ") + 6);
								if (startStr.contains(" ")) {
									startStr = startStr.substring(0, startStr.indexOf(" "));
								}
								if (startStr.endsWith(",")) {
									startStr = startStr.substring(0, startStr.length() - 1);
								}
								chapter.setStart(Double.parseDouble(startStr));
							}
							//set chapter end
							if (line.contains(" end ")) {
								String endStr = line.substring(line.indexOf(" end ") + 5);
								if (endStr.contains(" ")) {
									endStr = endStr.substring(0, endStr.indexOf(" "));
								}
								chapter.setEnd(Double.parseDouble(endStr));
							}
							chapter.setLang(DLNAMediaLang.UND);
							fFmpegMetaDataNr += 1;
							line = lines.get(fFmpegMetaDataNr);
							if (line.contains("Metadata:")) {
								fFmpegMetaDataNr += 1;
								line = lines.get(fFmpegMetaDataNr);
								while (line.indexOf("      ") == 0) {
									if (line.contains(": ")) {
										int aa = line.indexOf(": ");
										String key = line.substring(0, aa).trim();
										String value = line.substring(aa + 2);
										if ("title".equals(key)) {
											//do not set title if it is default, it will be filled automatically later
											if (!DLNAMediaChapter.isTitleDefault(value)) {
												chapter.setTitle(value);
											}
										} else {
											LOGGER.debug("New chapter metadata not handled \"" + key + "\" : \"" + value + "\"");
										}
										break;
									} else {
										fFmpegMetaDataNr += 1;
										line = lines.get(fFmpegMetaDataNr);
									}
								}
							}
							ffmpegChapters.add(chapter);
						}
						media.setChapters(ffmpegChapters);
					}
				}
			}
		}
		media.setFFmpegparsed(true);
	}

	private static Double parseDurationString(String duration) {
		return duration != null ? StringUtil.convertStringToTime(duration) : null;
	}
}
