public class LevenshteinData {
    public String Word;
    public float Score;

    public LevenshteinData(String WordIn, float ScoreIn) {
        Word = WordIn;
        Score = ScoreIn;
    }

    public void Out() {
        char[] WordWithCapitalizedFirstLetter = Word.toCharArray();
        WordWithCapitalizedFirstLetter[0] = Word.toUpperCase().charAt(0);
        System.out.printf("%s - %.2f%n", new String(WordWithCapitalizedFirstLetter), Score);
    }
}
