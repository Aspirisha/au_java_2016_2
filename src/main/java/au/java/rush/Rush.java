package au.java.rush;

import au.java.rush.commands.*;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;

import java.nio.file.Paths;

/**
 * Created by andy on 9/25/16.
 */
public class Rush {
    public static void main(String[] args) {
        String cwd = System.getProperty("user.dir");
        // this is needed for proper logging location
        System.setProperty("app.workdir", Paths.get(cwd).toAbsolutePath().toString());

        ArgumentParser parser = ArgumentParsers.newArgumentParser("rush")
                .defaultHelp(true)
                .description("rush is a simple yet unpowerful vcs.");
        Subparsers subparsers = parser.addSubparsers().help("sub-command help");

        Subparser parserBranch = subparsers.addParser("branch").help("create or delete a branch")
                .setDefault("func", new BranchCommand());
        parserBranch.addArgument("-d").action(Arguments.storeTrue());
        parserBranch.addArgument("branchName")
                .metavar("name")
                .type(String.class)
                .help("branch name");

        Subparser parserCheckout = subparsers.addParser("checkout")
                .aliases("co")
                .help("checkout branch or revision")
                .setDefault("func", new CheckoutCommand());
        parserCheckout.addArgument("branchOrRevision")
                .metavar("revision")
                .type(String.class)
                .help("branch name or revision hash code");

        Subparser parserCommit = subparsers.addParser("commit")
                .help("commit changes")
                .setDefault("func", new CommitCommand());
        parserCommit.addArgument("-m").required(true).metavar("message");

        Subparser parserLog = subparsers.addParser("log")
                .help("log help")
                .setDefault("func", new LogCommand());
        parserLog.addArgument("branchOrRevision")
                .metavar("revision")
                .type(String.class)
                .nargs("?")
                .help("branch name pr revision hash code")
                .setDefault((Object)null);

        Subparser parserMerge = subparsers.addParser("merge")
                .help("merge help")
                .setDefault("func", new MergeCommand());
        parserMerge.addArgument("branchOrRevision")
                .metavar("branch")
                .type(String.class)
                .help("branch or revision name");

        Subparser parserInit = subparsers.addParser("init")
                .help("init help")
                .setDefault("func", new InitCommand());

        Subparser parserAdd = subparsers.addParser("add").help("add help")
                .setDefault("func", new AddCommand());
        parserAdd.addArgument("fileOrDirectory")
                .metavar("file")
                .type(String.class)
                .help("file name to add to index");

        Subparser parserDiff = subparsers.addParser("diff").help("diff help")
                .setDefault("func", new DiffCommand());
        parserDiff.addArgument("revision")
                .metavar("revision")
                .type(String.class)
                .help("revision to diff with");

        try {
            Namespace ns = parser.parseArgs(args);
            ((Subcommand) ns.get("func")).execute(ns);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
    }
}
