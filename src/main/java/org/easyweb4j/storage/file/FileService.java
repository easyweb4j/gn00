package org.easyweb4j.storage.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;

public interface FileService {
  /**
   * 根据时间存储文件，生成目录**yyyy/MM/dd**, 文件名可选，默认为HHmmssSSS.{suffix}
   *
   * @param content  文件内容
   * @param fileName 文件名，可选
   * @param suffix   文件后缀，统一转成小写
   * @return 相对路径
   */
  Path storeFileUsingDate(ByteBuffer content, String fileName, String suffix) throws IOException;

  /**
   * 根据hash值存储文件， 生成目录**sha1[0:2]/sha1[2:4]/sha1., 文件名可选，默认为hash值
   *
   * @param content  文件内容
   * @param fileName 文件名，可选
   * @param suffix   文件后缀，统一转成小写
   * @return 相对路径
   * @throws IOException
   */
  Path storeFileUsingHash(ByteBuffer content, String fileName, String suffix) throws IOException;

  /**
   * 读取文件全部内容
   *
   * @param filePath 文件相对路径
   * @param charset 字节，默认utf-8
   * @return 字符串
   * @throws IOException
   */
  String read(Path filePath, Charset charset) throws IOException;

  /**
   * 读取文件成字节
   *
   * @param filePath 文件相对路径
   * @return 字节
   * @throws IOException
   */
  ByteBuffer read(Path filePath) throws IOException;
}
