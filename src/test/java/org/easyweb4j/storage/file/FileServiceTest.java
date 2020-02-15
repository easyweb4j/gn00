package org.easyweb4j.storage.file;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FileServiceTest extends AbstractFileService {

  @Override
  protected Path prefix() {
    return Paths.get("/tmp/file-service-unit-test");
  }

  private static FileService fileService;
  private static ByteBuffer content;

  @BeforeClass
  public static void setup() throws NoSuchFieldException, IllegalAccessException {
    fileService = new FileServiceTest();
    content = ByteBuffer.wrap(RandomUtils.nextBytes(16));
  }

  @AfterClass
  public static void destroy()
    throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method prefix = FileServiceTest.class.getDeclaredMethod("prefix", null);
    Path path = ((Path) prefix.invoke(fileService));
    FileUtils.cleanDirectory(path.toFile());
    FileUtils.deleteDirectory(path.toFile());
  }

  @Test
  public void storeByHash() throws IOException {
    Path path = fileService.storeFileUsingHash(content, null, "jpg");
    Assert.assertTrue(Files.exists(prefix().resolve(path)));
    Path fileDir = path.getParent();
    Matcher matcher = Pattern.compile("[a-z0-9]{2}/[a-z0-9]{2}", Pattern.DOTALL)
      .matcher(fileDir.toString());
    Assert.assertTrue(matcher.matches());
  }

  @Test
  public void storeByDate() throws IOException {
    Path path = fileService.storeFileUsingDate(content, "test", ".jpg");
    Assert.assertTrue(Files.exists(prefix().resolve(path)));
    Path fileDir = path.getParent();
    Matcher matcher = Pattern.compile("[0-9]{4}/[0-9]{2}/[0-9]{2}", Pattern.DOTALL)
      .matcher(fileDir.toString());
    Assert.assertTrue(matcher.matches());
  }

  @Test
  public void readString() throws IOException {
    String writeValue = "hello abc";
    byte[] writeValueBytes = writeValue.getBytes(Charset.defaultCharset());

    Path path = fileService.storeFileUsingDate(ByteBuffer.wrap(writeValueBytes), "test", ".txt");
    Assert.assertTrue(Files.exists(prefix().resolve(path)));

    String readStr = fileService.read(path, Charset.defaultCharset());
    Assert.assertEquals(writeValue, readStr);

    // write big string
    writeValue = RandomStringUtils.random(9048);
    writeValueBytes = writeValue.getBytes(Charset.defaultCharset());

    path = fileService.storeFileUsingDate(ByteBuffer.wrap(writeValueBytes), null, ".txt");
    Assert.assertTrue(Files.exists(prefix().resolve(path)));

    readStr = fileService.read(path, Charset.defaultCharset());
    Assert.assertEquals(writeValue, readStr);
  }
}
