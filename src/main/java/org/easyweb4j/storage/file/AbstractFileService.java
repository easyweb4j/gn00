package org.easyweb4j.storage.file;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 抽象的文件服务，定义本地文件存储
 *
 * @author ChenLei(chenlei @ lidig.com)
 * @date 2020/02/05
 * @since 1.0
 */
public abstract class AbstractFileService implements FileService {

  /**
   * 获取默认前缀路径
   *
   * @return 前缀路径
   */
  protected abstract Path prefix();

  @Override
  public Path storeFileUsingDate(ByteBuffer content, String fileName, String suffix)
    throws IOException {
    LocalDateTime now = LocalDateTime.now();
    String format = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd,HHmmssSSS"));
    int colonPos = format.indexOf(",");
    String dirPart = format.substring(0, colonPos);
    String fileNamePart = format.substring(colonPos + 1);

    Path relativeDirPath = null;
    for (String part : dirPart.split("-")) {
      if (null == relativeDirPath) {
        relativeDirPath = Paths.get(part);
        continue;
      }

      relativeDirPath = relativeDirPath.resolve(part);
    }

    String actualFileName =
      concatFileName(StringUtils.isBlank(fileName) ? fileNamePart : fileName, suffix);

    write2File(prefix().resolve(relativeDirPath), actualFileName, content);

    return relativeDirPath.resolve(actualFileName);
  }

  @Override
  public Path storeFileUsingHash(ByteBuffer content, String fileName, String suffix)
    throws IOException {
    // long(yyyyMMddHHmmssSSS) 8 bytes + random int(10000, 100000000) 4 bytes
    byte[] hashContent = new byte[12];
    ByteBuffer.wrap(hashContent)
      .putLong(Long.valueOf(LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
      )))
      .putInt(RandomUtils.nextInt(10000, 100000000));

    String hashDigestStr = DigestUtils.sha1Hex(hashContent).toLowerCase();

    Path relativePath = Paths.get(
      hashDigestStr.substring(0, 2),
      hashDigestStr.substring(2, 4)
    );
    String actualFileName =
      concatFileName(StringUtils.isBlank(fileName) ? hashDigestStr : fileName, suffix);

    write2File(prefix().resolve(relativePath), actualFileName, content);
    return relativePath.resolve(actualFileName);
  }

  private boolean createDirectories(Path creatingDir) throws IOException {
    if (!creatingDir.isAbsolute()) {
      return false;
    }

    if (!Files.exists(creatingDir)) {
      // TODO window 处理
      Files.createDirectories(creatingDir, defaultFileAttrs());
    }
    return true;
  }

  protected FileAttribute defaultFileAttrs() {
    return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr--r--"));
  }

  protected void write2File(Path absDirPath, String fileName, ByteBuffer content)
    throws IOException {
    Path absFiePath = absDirPath.resolve(fileName);
    if (Files.exists(absFiePath)) {
      throw new FileAlreadyExistsException(absFiePath.toString());
    }

    createDirectories(absDirPath);

    ByteBuffer dupedContent = content.duplicate();

    try (FileChannel channel = new FileOutputStream(absFiePath.toFile())
      .getChannel()) {
      channel.write(dupedContent);
    }
  }

  private String concatFileName(String fileNameWithOutSuffix, String suffix) {
    String sep = ".";
    return fileNameWithOutSuffix + sep + (sep.equals(suffix.substring(0, 1)) ? suffix.substring(1)
      : suffix);

  }

  @Override
  public String read(Path filePath, Charset charset) throws IOException {
    Charset actualCharset = null == charset ? Charset.forName("UTF-8") : charset;

    ByteBuffer content = readAllBytes(filePath);
    byte[] strBytes = new byte[content.limit()];
    content.get(strBytes);
    return new String(strBytes, actualCharset);
  }

  @Override
  public ByteBuffer read(Path filePath) throws IOException {
    return readAllBytes(filePath);
  }

  private ByteBuffer readAllBytes(Path filePath) throws IOException {
    int readSizeBatch = 4096;
    ByteBuffer content = ByteBuffer.allocate(readSizeBatch);

    try(ByteChannel byteChannel = Files.newByteChannel(prefix().resolve(filePath))) {
      while (0 < byteChannel.read(content) && !content.hasRemaining()) {
        // extend byte buffer
        ByteBuffer extendBuffer = ByteBuffer.allocate(content.capacity() + readSizeBatch);
        content.rewind();
        extendBuffer.put(content);
        content = extendBuffer;
      }
    }

    content.limit(content.position());
    content.rewind();
    return content;
  }
}
