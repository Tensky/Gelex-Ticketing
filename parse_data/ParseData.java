import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * ParseData Gelex
 */
public class ParseData {

    public static void main(String[] args) {
        System.out.println();
        System.out.println("PARSER TICKETING GELEX");
        System.out.println();
        System.out.println("Nama file:");

        Scanner sc = new Scanner(System.in);
        String filename = sc.nextLine();
        sc.close();

        File file = new File(filename + ".TXT");
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("File tidak ditemukan");
            return;
        }

        List<Entry> rawEntries = new ArrayList<>();
        List<Entry> noMultipleTapEntries = new ArrayList<>();
        List<Entry> noDuplicateEntries = new ArrayList<>();

        String temp = "";

        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            if (s.contains("$")) {
                rawEntries.add(new Entry(temp));
                temp = "";
            } else {
                s = s.concat("#");
                temp += s;
            }
        }
        scanner.close();

        String prevName = "";
        for (Entry e : rawEntries) {
            if (!e.nama.equals(prevName)) {
                noMultipleTapEntries.add(e);
                prevName = e.nama;
            }

            boolean exists = false;
            for (Entry entry : noDuplicateEntries) {
                if (entry.nama.equals(e.nama)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                noDuplicateEntries.add(e);
            }
        }

        Collections.sort(rawEntries);
        Collections.sort(noMultipleTapEntries);
        Collections.sort(noDuplicateEntries);

        try {
            FileWriter fileWriter = new FileWriter(filename + "_RAW.CSV");
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("Nama, NIU, Waktu");
            rawEntries.forEach(e -> {
                printWriter.println(e.cetak());
            });
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            FileWriter fileWriter = new FileWriter(filename + "_NO_MULTI_TAP.CSV");
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("Nama, NIU, Waktu");
            noMultipleTapEntries.forEach(e -> {
                printWriter.println(e.cetak());
            });
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            FileWriter fileWriter = new FileWriter(filename + "_NO_DUPLICATE.CSV");
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("Nama, NIU, Waktu");
            noDuplicateEntries.forEach(e -> {
                printWriter.println(e.cetak());
            });
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    static class Entry implements Comparable<Entry> {
        final String nama;
        final String nim;
        final String waktu;
        Date date;

        public Entry(String source) {
            String[] sources = source.split("#");
            nama = sources[sources.length - 3];
            String nimHexRaw = sources[sources.length - 2];
            String nimHex = "";
            nimHex += nimHexRaw.charAt(6);
            nimHex += nimHexRaw.charAt(7);
            nimHex += nimHexRaw.charAt(4);
            nimHex += nimHexRaw.charAt(5);
            nimHex += nimHexRaw.charAt(2);
            nimHex = nimHex.toUpperCase();
            nim = "" + Integer.parseInt(nimHex, 16);
            waktu = sources[sources.length - 1];
            try {
                date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(waktu.substring(waktu.indexOf(", ") + 2));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        public String cetak() {
            return "\"" + nama + "\", \"" + nim + "\", \"" + waktu + "\"";
        }

        @Override
        public int compareTo(Entry o) {
            return this.date.compareTo(o.date);
        }
    }
}