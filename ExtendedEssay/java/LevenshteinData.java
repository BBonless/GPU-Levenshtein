public class LevenshteinData {
    public String Word;
    public float Score;

    public LevenshteinData(String WordIn, float ScoreIn) {
        Word = WordIn;
        Score = ScoreIn;
    }

    public void Out() {
        System.out.println(
                Word + " " + Score
        );
    }
}
