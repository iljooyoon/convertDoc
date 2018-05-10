package convertDoc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.StringTokenizer;

public class Main {
	static private String MODE;
	private static File sourcePath;
	private static File outputDir;
	private static int cnt;
	private static int[] order;
	
	public static void main(String[] args) {
		if (args == null || args.length != 1) {
			printHelp();
			System.exit(0);
		}
		
		MODE = args[0];
		
		String[] settings = readSettingsFile();
		
		sourcePath = new File(settings[0]);
		outputDir = new File(settings[1]);
		
		if (!sourcePath.exists()) {
			System.err.println(sourcePath.getPath() + " ������ �������� �ʽ��ϴ�.");
			System.exit(0);
		}
		
		if ("clsf".equals(MODE.toLowerCase())) {
			clsfMain();
		}
		else if ("rearrange".equals(MODE.toLowerCase())) {
			cnt = Integer.parseInt(settings[2]);
			order = setOrders(settings[3]);
			
			rearrangeMain();
		}
		else {
			printHelp();
			System.exit(0);
		}
	}

	private static void clsfMain() {
		for (File f : sourcePath.listFiles()) {
			if (f.isFile() && isTargetFile(f.getName())) {
				if (!outputDir.exists())
					outputDir.mkdirs();
				
				boolean isHOGA = f.getName().indexOf("HOGA") >= 0;

				try (BufferedReader br = new BufferedReader(new FileReader(f))) {
					String line;

					while ((line = br.readLine()) != null) {
						String[] data = line.split(",");
						String filename;

						// ȣ�� : HOGA => �����ڵ�_orderbook
						if (isHOGA) {
							filename = f.getName().replace("HOGA", data[0] + "_orderbook");
						}
						// ü�� : EXCUTED => �����ڵ�_executed
						else {
							filename = f.getName().replace("EXCUTED", data[0] + "_executed");
						}
						
						String targetName = outputDir.getAbsolutePath() + File.separator + filename; // �̸� ����. YYYY-MM-DD_�����ȣ_HOGA
						
						try(BufferedWriter bw = new BufferedWriter(new FileWriter(targetName, true))) {
							bw.write(line);
							bw.newLine();
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				System.out.println(f.getName() + " is split.");
			}
		}
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
						}
						else {
							System.err.println("������ ���� ������ ��ġ���� �ʽ��ϴ�. (column count : " + data.length + ", settings value : " + cnt + "\ndata : " + line);
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
		System.out.println("[����]");
		System.out.println("java -jar convertDoc.jar {mode}");
		System.out.println("��) java -jar convertDoc.jar clsf");
		System.out.println();
		System.out.println("�� mode ����");
		System.out.println("clsf : ������ �����ڵ� ���� �з�");
		System.out.println("rearrange : ���� ���� �÷� ������ ����");
		System.out.println();
		System.out.println("�� settings.ini ���� ���� �ʿ�");
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

			if (!("clsf".equals(MODE) && i >= 2 || "rearrange".equals(MODE) && i >= 4)) {
				System.err.println(confFile + " ���� ���� ����");
				System.exit(0);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return settings;
	}
}
