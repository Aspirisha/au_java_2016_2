package au.java.rush;

import au.java.rush.commands.*;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;

/**
 * Created by andy on 9/25/16.
 */
public class Rush {
    public static void main(String[] args1) {
        String[] args = {"branch", "fff"};

        ArgumentParser parser = ArgumentParsers.newArgumentParser("Checksum")
                .defaultHelp(true)
                .description("Calculate checksum of given files.");
        Subparsers subparsers = parser.addSubparsers().help("sub-command help");

        Subparser parserBranch = subparsers.addParser("branch").help("branch help")
                .setDefault("func", new BranchCommand());
        parserBranch.addArgument("-d").action(Arguments.storeTrue());
        parserBranch.addArgument("branchName")
                .metavar("N")
                .type(String.class)
                .help("branch name");

        Subparser parserCheckout = subparsers.addParser("checkout")
                .aliases("co")
                .help("checkout help")
                .setDefault("func", new CheckoutCommand());
        parserCheckout.addArgument("branchOrRevision")
                .metavar("N")
                .type(String.class)
                .help("branch or revision name");

        Subparser parserCommit = subparsers.addParser("commit")
                .help("commit help")
                .setDefault("func", new CommitCommand());
        parserCommit.addArgument("-m");

        Subparser parserLog = subparsers.addParser("log")
                .help("log help")
                .setDefault("func", new LogCommand());

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
