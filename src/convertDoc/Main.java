package convertDoc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class Main {
	private static String MODE;
	private static Map<String, ArrayList<String>> TEXT_MAP = new HashMap<String, ArrayList<String>>();
	private static File sourcePath = null;
	private static File outputDir = null;
	private static File moveDir = null;
	private static int cnt;
	private static int[] order;

	public static void main(String[] args) {
		if (args == null || args.length != 1) {
			printHelp();
			System.exit(0);
		}

		MODE = args[0];

		Map<String, String> settings = readSettingsFile();

		sourcePath = new File(getValue(settings, "INPUT_FOLDER"));
		outputDir = new File(getValue(settings, "OUTPUT_FOLDER"));

		if (settings.containsKey("MOVE_FOLDER")) {
			moveDir = new File(settings.get("MOVE_FOLDER"));
		}

		if (!sourcePath.exists()) {
			System.err.println(sourcePath.getPath() + " 폴더가 존재하지 않습니다.");
			System.exit(0);
		}

		if ("clsf".equals(MODE.toLowerCase())) {
			clsfMain();
		} else if ("rearrange".equals(MODE.toLowerCase())) {
			cnt = Integer.parseInt(getValue(settings, "COLUMN_COUNT"));
			order = setOrders(getValue(settings, "ORDER"));

			rearrangeMain();
		} else {
			printHelp();
			System.exit(0);
		}
	}

	private static String getValue(Map<String, String> settings, String key) {
		if (settings.containsKey(key)) {
			return settings.get(key);
		} else {
			throw new RuntimeException(key + " 가 입력되지 않았습니다.");
		}
	}

	private static void clsfMain() {
		for (File f : sourcePath.listFiles()) {
			if (f.isFile() && isTargetFile(f.getName())) {
				if (!outputDir.exists()) {
					if (!outputDir.mkdirs()) {
						throw new RuntimeException("Folder Creation Fail.");
					}
				}

				boolean isHOGA = f.getName().indexOf("HOGA") >= 0;

				try (BufferedReader br = new BufferedReader(new FileReader(f))) {
					String line;

					int cnt = 0;

					while ((line = br.readLine()) != null) {
						String[] data = line.split(",");
						String filename;

						// 호가 : HOGA => 종목코드_orderbook
						if (isHOGA) {
							filename = f.getName().replace("HOGA", data[0] + "_orderbook");
						}
						// 체결 : EXCUTED => 종목코드_executed
						else {
							filename = f.getName().replace("EXCUTED", data[0] + "_executed");
						}

						String key = outputDir.getAbsolutePath() + File.separator + filename;

						if (TEXT_MAP.containsKey(key)) {
							TEXT_MAP.get(key).add(line);
						} else {
							ArrayList<String> value = new ArrayList<String>();

							value.add(line);

							TEXT_MAP.put(key, value);
						}

						if (cnt++ > 100000) {
							cnt = 0;
							writeTextMap();
						}
					}
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					writeTextMap();
				}
				System.out.println(f.getName() + " is split.");

				moveFile();
			}
		}
	}

	private static void moveFile() {
		try {
			// 결과 파일 이동 옵션
			if (moveDir != null) {
				for (File source : outputDir.listFiles()) {
					File target = new File(moveDir.getAbsolutePath() + File.separator + source.getName());
					
					int retry = 0;
					while (true) {
						try {
							if (target.createNewFile()) {
								break;
							}
							retry++;
							Thread.sleep(200);
							
							if (retry > 10) {
								throw new RuntimeException("File create error!");
							}
						} catch (Exception e) {
							retry++;
							
							if (retry > 10) {
								throw new RuntimeException("File move error!");
							}
						}
					}
					
					try (BufferedReader br = new BufferedReader(new FileReader(source))) {
						try (BufferedWriter bw = new BufferedWriter(new FileWriter(target))) {
							String line;
							
							while ((line = br.readLine()) != null) {
								while (true) {
									try {
										bw.write(line + "\n");
										break;
									} catch (Exception e) {
										retry++;
										
										if (retry > 10) {
											throw new RuntimeException("File move error!");
										}
									}
								}
							}
							source.delete();
							
							System.out.println(source.getName() + " file is moved.");
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void writeTextMap() {
		for (String targetName : TEXT_MAP.keySet()) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(targetName, true))) {
				for (String text : TEXT_MAP.get(targetName)) {
					bw.write(text);
					bw.newLine();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		TEXT_MAP.clear();
	}

	private static boolean isTargetFile(String filename) {
		return filename.toLowerCase().endsWith(".csv")
				&& (filename.indexOf("KOSDAQ") >= 0 || filename.indexOf("KOSPI") >= 0)
				&& (filename.indexOf("HOGA") >= 0 || filename.indexOf("EXCUTED") >= 0);
	}

	private static void rearrangeMain() {
		StringBuffer sb = new StringBuffer();

		int convertCnt = 0;

		for (File f : sourcePath.listFiles()) {
			if (f.isFile() && f.getName().toLowerCase().endsWith(".csv")) {
				if (!outputDir.exists())
					outputDir.mkdirs();

				String targetName = outputDir.getAbsolutePath() + File.separator + f.getName();
				try (BufferedReader br = new BufferedReader(new FileReader(f));
						BufferedWriter bw = new BufferedWriter(new FileWriter(targetName))) {
					String line;

					while ((line = br.readLine()) != null) {
						String[] data = line.split(",");

						if (data.length == cnt) {
							sb.setLength(0);

							for (int i = 0; i < order.length; i++) {
								sb.append(data[order[i]]);

								if (i + 1 != order.length) {
									sb.append(",");
								}
							}
							bw.write(sb.toString());
							bw.newLine();
						} else {
							System.err.println("데이터 열의 갯수가 일치하지 않습니다. (column count : " + data.length
									+ ", settings value : " + cnt + "\ndata : " + line);
							System.exit(0);
						}
					}
				} catch (Exception e) {
					System.err.println(targetName + " error.");
					throw new RuntimeException(e);
				}
				System.out.println(targetName + " created.");
				convertCnt++;
			}
		}

		if (convertCnt == 0)
			System.out.println("There is no file in " + sourcePath.getPath());
		else
			System.out.println(convertCnt + " files created.");
	}

	private static void printHelp() {
		System.out.println("[사용법]");
		System.out.println("java -jar convertDoc.jar {mode}");
		System.out.println("예) java -jar convertDoc.jar clsf");
		System.out.println();
		System.out.println("※ mode 종류");
		System.out.println("clsf : 파일을 종목코드 별로 분류");
		System.out.println("rearrange : 파일 내의 컬럼 순서를 변경");
		System.out.println();
		System.out.println("※ settings.ini 파일 설정 필요");
	}

	private static int[] setOrders(String orderString) {
		StringTokenizer st = new StringTokenizer(orderString, ",");
		int[] orders = new int[st.countTokens()];

		for (int i = 0; st.hasMoreTokens(); i++) {
			orders[i] = Integer.parseInt(st.nextToken());
		}
		return orders;
	}

	private static Map<String, String> readSettingsFile() {
		File confFile = new File("settings.ini");

		if (!confFile.exists()) {
			System.err.println("cannot found settings.ini");
			System.exit(0);
		}

		Map<String, String> settings = new HashMap<String, String>();

		try (BufferedReader br = new BufferedReader(new FileReader(confFile))) {
			String text;

			while ((text = br.readLine()) != null) {
				String trimedStr = text.trim();
				if (!trimedStr.isEmpty() && !trimedStr.startsWith("#")) {
					String[] split = trimedStr.split("=");

					settings.put(split[0], split[1]);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return settings;
	}
}
