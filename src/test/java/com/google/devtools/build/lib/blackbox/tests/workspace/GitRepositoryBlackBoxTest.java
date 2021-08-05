// Copyright 2019 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.devtools.build.lib.blackbox.tests.workspace;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.build.lib.blackbox.tests.workspace.RepoWithRuleWritingTextGenerator.callRule;
import static com.google.devtools.build.lib.blackbox.tests.workspace.RepoWithRuleWritingTextGenerator.loadRule;

import com.google.devtools.build.lib.blackbox.framework.BlackBoxTestContext;
import com.google.devtools.build.lib.blackbox.framework.BuilderRunner;
import com.google.devtools.build.lib.blackbox.framework.PathUtils;
import com.google.devtools.build.lib.blackbox.framework.ProcessResult;
import com.google.devtools.build.lib.blackbox.junit.AbstractBlackBoxTest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

/**
 * Black box tests for git_repository/new_git_repository. On Windows, runs without MSYS {@link
 * WorkspaceTestUtils#bazel}
 *
 * <p>General approach to testing:
 *
 * <p>We use {@link GitRepositoryHelper} and {@link RepoWithRuleWritingTextGenerator} helper
 * classes.
 *
 * <p>1. We are creating some git repository with the preset contents, which will be used for
 * fetching contents for the test. We plan to fetch contents specifying either commit hash, tag, or
 * branch. For all test variants, we are creating the same repository, as the same HEAD commit is
 * marked with a tag, and can be addressed with commit hash, master branch, and tag name.
 *
 * <p>2. The contents of the git repository working tree is generated by {@link
 * RepoWithRuleWritingTextGenerator}. We pass some certain text to that generator; that exact text
 * should appear as a result of the build of the generated target "call_write_text" in the file
 * "out.txt".
 *
 * <p>3. We generate the new_git_repository repository rule, which refers to the git repository,
 * created in #1, specifying different git repository attributes in each test. We call 'bazel build'
 * for the "call_write_text" target of the external repository, and asserting the contents in the
 * "out.txt" file.
 */
public class GitRepositoryBlackBoxTest extends AbstractBlackBoxTest {
  private static final String HELLO_FROM_EXTERNAL_REPOSITORY = "Hello from GIT repository!";
  private static final String HELLO_FROM_BRANCH = "Hello from branch!";

  /**
   * Tests usage of new_git_repository workspace rule with the "tag" attribute. Please see the
   * general approach description in the class javadoc comment.
   */
  @Test
  public void testCloneAtTag() throws Exception {
    Path repo = context().getTmpDir().resolve("ext_repo");
    setupGitRepository(context(), repo);

    String buildFileContent =
        String.format(
            "%s\n%s",
            loadRule(""), callRule("call_write_text", "out.txt", HELLO_FROM_EXTERNAL_REPOSITORY));
    context()
        .write(
            "WORKSPACE",
            "load(\"@bazel_tools//tools/build_defs/repo:git.bzl\", \"new_git_repository\")",
            "new_git_repository(",
            "  name='ext',",
            String.format("  remote='%s',", PathUtils.pathToFileURI(repo.resolve(".git"))),
            "  tag='first',",
            String.format("  build_file_content=\"\"\"%s\"\"\",", buildFileContent),
            ")");

    // This creates Bazel without MSYS, see implementation for details.
    BuilderRunner bazel = WorkspaceTestUtils.bazel(context());
    bazel.build("@ext//:call_write_text");
    Path outPath = context().resolveBinPath(bazel, "external/ext/out.txt");
    WorkspaceTestUtils.assertLinesExactly(outPath, HELLO_FROM_EXTERNAL_REPOSITORY);
  }

  /**
   * Tests usage of new_git_repository workspace rule with the "commit" attribute. Please see the
   * general approach description in the class javadoc comment.
   */
  @Test
  public void testCloneAtCommit() throws Exception {
    Path repo = context().getTmpDir().resolve("ext_repo");
    String commit = setupGitRepository(context(), repo);

    String buildFileContent =
        String.format(
            "%s\n%s",
            loadRule(""), callRule("call_write_text", "out.txt", HELLO_FROM_EXTERNAL_REPOSITORY));
    context()
        .write(
            "WORKSPACE",
            "load(\"@bazel_tools//tools/build_defs/repo:git.bzl\", \"new_git_repository\")",
            "new_git_repository(",
            "  name='ext',",
            String.format("  remote='%s',", PathUtils.pathToFileURI(repo.resolve(".git"))),
            String.format("  commit='%s',", commit),
            String.format("  build_file_content=\"\"\"%s\"\"\",", buildFileContent),
            ")");

    // This creates Bazel without MSYS, see implementation for details.
    BuilderRunner bazel = WorkspaceTestUtils.bazel(context());
    bazel.build("@ext//:call_write_text");
    Path outPath = context().resolveBinPath(bazel, "external/ext/out.txt");
    WorkspaceTestUtils.assertLinesExactly(outPath, HELLO_FROM_EXTERNAL_REPOSITORY);
  }

  /**
   * Tests usage of new_git_repository workspace rule with the "branch" attribute. Please see the
   * general approach description in the class javadoc comment.
   */
  @Test
  public void testCloneAtMaster() throws Exception {
    Path repo = context().getTmpDir().resolve("ext_repo");
    setupGitRepository(context(), repo);

    String buildFileContent =
        String.format(
            "%s\n%s",
            loadRule(""), callRule("call_write_text", "out.txt", HELLO_FROM_EXTERNAL_REPOSITORY));
    context()
        .write(
            "WORKSPACE",
            "load(\"@bazel_tools//tools/build_defs/repo:git.bzl\", \"new_git_repository\")",
            "new_git_repository(",
            "  name='ext',",
            String.format("  remote='%s',", PathUtils.pathToFileURI(repo.resolve(".git"))),
            "  branch='master',",
            String.format("  build_file_content=\"\"\"%s\"\"\",", buildFileContent),
            ")");

    // This creates Bazel without MSYS, see implementation for details.
    BuilderRunner bazel = WorkspaceTestUtils.bazel(context());
    bazel.build("@ext//:call_write_text");
    Path outPath = context().resolveBinPath(bazel, "external/ext/out.txt");
    WorkspaceTestUtils.assertLinesExactly(outPath, HELLO_FROM_EXTERNAL_REPOSITORY);
  }

  /**
   * Tests usage of git_repository workspace rule in the particular use case, when only the commit
   * hash is specified, and the commit is not in the HEAD-reachable subtree, on a separate branch.
   */
  @Test
  public void testCheckoutOfCommitFromBranch() throws Exception {
    Path repo = context().getTmpDir().resolve("branch_repo");
    GitRepositoryHelper gitRepository = initGitRepository(context(), repo);

    context().write(repo.resolve("master.marker").toString());
    gitRepository.addAll();
    gitRepository.commit("Initial commit");

    gitRepository.createNewBranch("demonstrate_branch");

    RepoWithRuleWritingTextGenerator generator = new RepoWithRuleWritingTextGenerator(repo);
    generator.withOutputText(HELLO_FROM_BRANCH).setupRepository();

    gitRepository.addAll();
    gitRepository.commit("Commit in branch");
    String branchCommitHash = gitRepository.getHead();

    gitRepository.checkout("master");
    generator.withOutputText(HELLO_FROM_EXTERNAL_REPOSITORY).setupRepository();
    gitRepository.addAll();
    gitRepository.commit("Commit in master");

    context()
        .write(
            "WORKSPACE",
            "load(\"@bazel_tools//tools/build_defs/repo:git.bzl\", \"git_repository\")",
            "git_repository(",
            "  name='ext',",
            String.format("  remote='%s',", PathUtils.pathToFileURI(repo.resolve(".git"))),
            String.format("  commit='%s',", branchCommitHash),
            ")");

    // This creates Bazel without MSYS, see implementation for details.
    BuilderRunner bazel = WorkspaceTestUtils.bazel(context());
    bazel.build("@ext//:write_text");
    Path outPath = context().resolveBinPath(bazel, "external/ext/out");
    WorkspaceTestUtils.assertLinesExactly(outPath, HELLO_FROM_BRANCH);
  }

  /**
   * Tests usage of git_repository workspace rule in the particular use case, when only the commit
   * hash is specified, and the commit is not in the HEAD-reachable subtree, on a separate tag and
   * not on any branch.
   */
  @Test
  public void testCheckoutOfCommitFromTag() throws Exception {
    Path repo = context().getTmpDir().resolve("tag_repo");
    GitRepositoryHelper gitRepository = initGitRepository(context(), repo);

    context().write(repo.resolve("master.marker").toString());
    gitRepository.addAll();
    gitRepository.commit("Initial commit");

    gitRepository.createNewBranch("demonstrate_branch");

    RepoWithRuleWritingTextGenerator generator = new RepoWithRuleWritingTextGenerator(repo);
    generator.withOutputText(HELLO_FROM_BRANCH).setupRepository();

    gitRepository.addAll();
    gitRepository.commit("Commit in tag1");
    gitRepository.tag("tag1");
    String tagCommitHash = gitRepository.getHead();

    gitRepository.checkout("master");

    // delete branch, so that the last commit is not an any branch.
    gitRepository.deleteBranch("demonstrate_branch");

    generator.withOutputText(HELLO_FROM_EXTERNAL_REPOSITORY).setupRepository();
    gitRepository.addAll();
    gitRepository.commit("Commit in master");

    context()
        .write(
            "WORKSPACE",
            "load(\"@bazel_tools//tools/build_defs/repo:git.bzl\", \"git_repository\")",
            "git_repository(",
            "  name='ext',",
            String.format("  remote='%s',", PathUtils.pathToFileURI(repo.resolve(".git"))),
            String.format("  commit='%s',", tagCommitHash),
            ")");

    // This creates Bazel without MSYS, see implementation for details.
    BuilderRunner bazel = WorkspaceTestUtils.bazel(context());
    bazel.build("@ext//:write_text");
    Path outPath = context().resolveBinPath(bazel, "external/ext/out");
    WorkspaceTestUtils.assertLinesExactly(outPath, HELLO_FROM_BRANCH);
  }

  /** Tests that the error message is produced if the git repository does not exist. */
  @Test
  public void testGitRepositoryErrorMessage() throws Exception {
    context()
        .write(
            "WORKSPACE",
            "load(\"@bazel_tools//tools/build_defs/repo:git.bzl\", \"git_repository\")",
            "git_repository(",
            "  name='ext',",
            "  remote='file:///some_path',",
            "  commit='some_hash',",
            ")");

    // This creates Bazel without MSYS, see implementation for details.
    BuilderRunner bazel = WorkspaceTestUtils.bazel(context());
    ProcessResult result = bazel.shouldFail().build("@ext//:write_text");
    assertThat(result.errString()).contains("error running 'git fetch");
    assertThat(result.errString()).contains("fatal: Could not read from remote repository.");
  }

  private static String setupGitRepository(BlackBoxTestContext context, Path repo)
      throws Exception {
    GitRepositoryHelper gitRepository = initGitRepository(context, repo);

    RepoWithRuleWritingTextGenerator generator = new RepoWithRuleWritingTextGenerator(repo);
    generator.skipBuildFile().setupRepository();

    gitRepository.addAll();
    gitRepository.commit("Initial commit");
    gitRepository.tag("first");
    return gitRepository.getHead();
  }

  private static GitRepositoryHelper initGitRepository(BlackBoxTestContext context, Path repo)
      throws Exception {
    PathUtils.deleteTree(repo);
    Files.createDirectories(repo);
    GitRepositoryHelper gitRepository = new GitRepositoryHelper(context, repo);
    gitRepository.init();
    return gitRepository;
  }
}
