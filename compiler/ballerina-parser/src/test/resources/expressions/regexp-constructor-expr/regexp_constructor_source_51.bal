public function main() {
    _ = re `$*`;
    _ = re `${1,2}`;
    _ = re `${2,1}`;
    _ = re `\\2(a)(`;
    _ = re `(?a`;
    _ = re `(?a)`;
    _ = re `(?:`;
    _ = re `(?:a`;
    _ = re `(:a`;
    _ = re `[b-a]`;
    _ = re `[a-b--+]`;
    _ = re `[\\u0001-\\u0000]`;
    _ = re `[\\u{1}-\\u{2}]`;
    _ = re `[\\u{2}-\\u{1}]`;
    _ = re `[\\z-\\a]`;
    _ = re `[0-9--+]`;
    _ = re `[\\c-a]`;
    _ = re `[©-€]`;
    _ = re `[€-©]`;
    _ = re `[\\uD834\\uDF06-\\uD834\\uDF08a-z]`;
}
