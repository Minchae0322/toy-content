package com.example.toycontent.app.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class YoutubeUtils {

  private static final String VIDEO_ID_REGEX =
      "(?:youtube\\.com/(?:watch\\?.*v=|embed/|shorts/|live/)|youtu\\.be/)([a-zA-Z0-9_-]{11})";

  private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(VIDEO_ID_REGEX);

  /**
   * YouTube URL에서 videoId(11자리)를 추출한다.
   *
   * 지원 URL 형식:
   * - https://www.youtube.com/watch?v=dQw4w9WgXcQ
   * - https://youtu.be/dQw4w9WgXcQ
   * - https://youtube.com/shorts/dQw4w9WgXcQ
   * - https://www.youtube.com/embed/dQw4w9WgXcQ
   * - https://www.youtube.com/live/dQw4w9WgXcQ
   * - https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=120
   *
   * @param url YouTube URL
   * @return videoId (11자리)
   * @throws IllegalArgumentException 유효하지 않은 URL인 경우
   */
  public static String extractVideoId(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("YouTube URL은 필수입니다");
    }

    Matcher matcher = VIDEO_ID_PATTERN.matcher(url.trim());
    if (!matcher.find()) {
      throw new IllegalArgumentException("유효하지 않은 YouTube URL입니다: " + url);
    }

    return matcher.group(1);
  }

  /**
   * YouTube videoId로 썸네일 URL을 생성한다.
   *
   * 화질 옵션:
   * - default.jpg     (120x90)
   * - mqdefault.jpg    (320x180)
   * - hqdefault.jpg    (480x360)
   * - sddefault.jpg    (640x480)
   * - maxresdefault.jpg (1280x720, 없을 수 있음)
   */
  public static String getThumbnailUrl(String videoId) {
    return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
  }

  /** YouTube 임베드 URL 생성 */
  public static String getEmbedUrl(String videoId) {
    return "https://www.youtube.com/embed/" + videoId;
  }

  /** 유효한 YouTube URL인지 검증 */
  public static boolean isValidUrl(String url) {
    if (url == null || url.isBlank()) {
      return false;
    }
    return VIDEO_ID_PATTERN.matcher(url.trim()).find();
  }
}
