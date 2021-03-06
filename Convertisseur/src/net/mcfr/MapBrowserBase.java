package net.mcfr;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MapBrowserBase {
  /** Mode debug. */
  public static final boolean DEBUG = false;

  private final String regionDirectory;
  private final Stack<Point> regionsCoordinates;
  private final int filesNumber;
  protected final Stack<Thread> threads;
  private int progress;
  private boolean started;
  private long startTime;
  private boolean interrupted;
  private boolean finished;

  /**
   * Crée un navigateur pour la carte donnée.
   * 
   * @param regionDirectory le chemin vers le dossier region (contient aussi le fichier level.dat)
   * @param threadsNb le nombre de threads
   * @throws IOException si une erreur de lecture/écriture est survenue
   */
  public MapBrowserBase(String regionDirectory, int threadsNb) throws IOException {
    this.regionDirectory = regionDirectory;
    this.regionsCoordinates = getRegionsCoordinates();
    this.filesNumber = this.regionsCoordinates.size();
    this.threads = new Stack<>();
    this.progress = 0;
    this.started = false;
    this.startTime = 0;
    this.interrupted = false;
    this.finished = false;
  }

  public void start() {
    this.progress = 0;
    this.started = true;
    this.startTime = System.currentTimeMillis();
    this.interrupted = false;
    this.finished = false;

    this.threads.forEach(thread -> thread.start());
  }

  /**
   * @return les coordonées de la prochaine région à convertir
   */
  public synchronized Optional<Point> getNextRegionCoordinate() {
    return this.regionsCoordinates.isEmpty() ? Optional.empty() : Optional.of(this.regionsCoordinates.pop());
  }

  /**
   * @return le chemin du dossier contenant le dossier 'region'
   */
  public String getRegionDirectory() {
    return this.regionDirectory;
  }

  public boolean isFinished() {
    return this.finished;
  }

  public float getProgress() {
    return ((float) this.progress / this.filesNumber) * 100;
  }

  /**
   * Met à jour et affiche la progression de la conversion.
   * 
   * @param display indique si la progression doit être affichée
   */
  public synchronized void updateProgress(boolean display) {
    this.progress++;
    if (display)
      System.out.format(Locale.ENGLISH, "Progression : %.2f%%\n", getProgress());
  }

  /**
   * Interrompt tous les threads.
   * 
   * @param threadId l'ID du threads appelant
   */
  public synchronized void interruptAllThreads(long threadId) {
    if (this.started && !this.interrupted) {
      this.threads.stream().filter(thread -> thread.getId() != threadId).forEach(thread -> thread.interrupt());
      this.interrupted = true;
    }
  }

  /**
   * Avertit qu'un thread a terminé son travail. Lorsque le dernier thread termine, le temps total
   * de conversion est affiché.
   * 
   * @param threadId l'ID du thread
   */
  public synchronized void notifyFinished(long threadId) {
    if (this.started && !this.threads.isEmpty()) {
      this.threads.removeIf(thread -> thread.getId() == threadId);
    }
    if (this.threads.isEmpty()) {
      long rawTime = System.currentTimeMillis() - this.startTime;
      long hours = rawTime / (3600 * 1000);
      long minutes = (rawTime / (60 * 1000)) % 60;
      long seconds = (rawTime / 1000) % 60;
      long mseconds = rawTime % 1000;

      if (this.threads.isEmpty()) {
        if (hours != 0)
          System.out.println(String.format("Terminé en %d h %d min %d.%d s.", hours, minutes, seconds, mseconds));
        else if (minutes != 0)
          System.out.println(String.format("Terminé en %d min %d.%d s.", minutes, seconds, mseconds));
        else if (seconds != 0)
          System.out.println(String.format("Terminé en %d.%d s.", seconds, mseconds));
        else
          System.out.println(String.format("Terminé en %d ms.", mseconds));
      }
      this.finished = true;
    }
  }

  /**
   * Retourne les coordonnées de toutes les régions à convertir.
   * 
   * @return la pile des coordonnées
   * @throws IOException si la lecture echoue
   */
  private Stack<Point> getRegionsCoordinates() throws IOException {
    Stack<Point> points = new Stack<>();
    Pattern pattern = Pattern.compile("r.(-?\\d+).(-?\\d+).mca");

    Files.list(Paths.get(getRegionDirectory() + "region")).forEach(p -> {
      Matcher m = pattern.matcher(p.getFileName().toString());
      if (m.find()) {
        points.push(new Point(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
      }
    });

    return points;
  }
}
