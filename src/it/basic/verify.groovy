File buildLog = new File(basedir, 'build.log')
assert buildLog.exists()
assert buildLog.text.contains("BUILD SUCCESS")
assert new File(basedir, 'target').exists();
assert new File(basedir, 'target/generated-sources/generated-version').exists();
assert new File(basedir, 'target/generated-test-sources/generated-test-version').exists();
