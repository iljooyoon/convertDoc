package convertDoc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.StringTokenizer;

public class Main {
	public static void main(String[] args) {
		String[] settings = readSettingsFile();

		File sourcePath = new File(settings[0]);
		File outputDir = new File(settings[1]);
		int cnt = Integer.parseInt(settings[2]);
		int[] order = setOrders(settings[3]);

		StringBuffer sb = new StringBuffer();

		if (!sourcePath.exists()) {
			System.err.println(sourcePath.getPath() + " 폴더가 존재하지 않습니다.");
			System.exit(0);
		}
		
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
						}
						else {
							System.err.println("데이터 열의 갯수가 일치하지 않습니다. (column count : " + data.length + ", settings value : " + cnt + "\ndata : " + line);
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

	private static int[] setOrders(String orderString) {
		StringTokenizer st = new StringTokenizer(orderString, ",");
		int[] orders = new int[st.countTokens()];

		for (int i = 0; st.hasMoreTokens(); i++) {
			orders[i] = Integer.parseInt(st.nextToken());
		}
		return orders;
	}

	private static String[] readSettingsFile() {
		File confFile = new File("settings.ini");

		if (!confFile.exists()) {
			System.err.println("cannot found settings.ini");
			System.exit(0);
		}

		String[] settings = new String[4];

		try (BufferedReader br = new BufferedReader(new FileReader(confFile))) {
			String text;

			int i = 0;

			while ((text = br.readLine()) != null) {
				if (!text.trim().startsWith("#")) {
					settings[i++] = text;
				}
			}

			if (i != 4) {
				System.err.println(confFile + " 설정 형식 오류");
				System.exit(0);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return settings;
	}
}
