package aonuchin.ctf;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.eclipse.jgit.lib.Constants.CHARSET;
import static org.eclipse.jgit.lib.Constants.OBJ_COMMIT;
import static org.eclipse.jgit.lib.Constants.encodeASCII;

public class FuckGit {

    public static final String DIFFICULTY = "000000004fffffffffffffffffffffffffffffff";

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: aonuchin.ctf.FuckGit tree parent threadsNum");
        }
        String tree = args[0];
        String parent = args[1];
        int threads = Integer.parseInt(args[2]);
        long time = System.currentTimeMillis() / 1000;
        body = "tree " + tree + "\n" +
                "parent " + parent + "\n" +
                "author CTF user <onuchinart@gmail.com> " + time + " +0000\n" +
                "committer CTF user <onuchinart@gmail.com> " + time + " +0000\n" +
                "\n" +
                "Give me gitcoin\n" +
                "\n";
        byte[] txt = encodeASCII(body);
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < threads; i++) {
            int threadNum = i;
            executorService.submit((Runnable) () -> mine(txt, threadNum));
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
        if (commit == null) {
            throw new TimeoutException("Failed to mine!");
        }
        System.out.print(body);
        System.out.print(commit);

    }
    private static volatile String commit = null;
    private static String body;

    public static void mine(byte[] commitText, int threadNum){
        int tryNum = 0;
        ObjectInserter.Formatter fmt = new ObjectInserter.Formatter();
        while (commit == null) {
            byte[] tryText = encodeASCII(threadNum + "-" + tryNum + "\n");

            ObjectId objectId = null;
            try (InputStream is  = new SequenceInputStream(
                    new ByteArrayInputStream(commitText),
                    new ByteArrayInputStream(tryText))) {
                objectId = fmt.idFor(OBJ_COMMIT, commitText.length + tryText.length, is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String hash = objectId.getName();
            if (hash.compareTo(DIFFICULTY) < 1) {
                commit = new String(tryText, StandardCharsets.US_ASCII);
            }
            tryNum++;
        }
    }
}
