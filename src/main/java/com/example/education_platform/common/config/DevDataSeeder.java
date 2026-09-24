package com.example.education_platform.common.config;

import com.example.education_platform.course.entity.Chapter;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.entity.Lesson;
import com.example.education_platform.course.entity.LessonCompletion;
import com.example.education_platform.course.entity.LessonType;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.course.repository.LessonCompletionRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty dev database so Swagger has something to show. Never runs under any other
 * profile, and never touches a database that already holds users or modules.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final String DEMO_PASSWORD = "Passw0rd-Demo";

    private final UserRepository users;
    private final CourseRepository courses;
    private final LessonCompletionRepository completions;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0 || courses.count() > 0) {
            LOG.info("Dev seed skipped — the database already holds data");
            return;
        }

        users.save(admin("Karim Haddad", "prof@eduflow.dz"));
        User amira = users.save(student("Amira Benali", "amira@eduflow.dz", Level.THIRD_AS));
        User yacine = users.save(student("Yacine Cherif", "yacine@eduflow.dz", Level.THIRD_AS));
        users.save(student("Lina Boudiaf", "lina@eduflow.dz", Level.THIRD_AS));
        users.save(student("Sofiane Meziane", "sofiane@eduflow.dz", Level.SECOND_AS));
        users.save(student("Nadia Cherifi", "nadia@eduflow.dz", Level.SECOND_AS));

        Course algo = courses.save(algorithmique());
        courses.save(basesDeDonnees());
        courses.save(initiation());
        courses.save(bureautique());

        // Different progress per student, so the percentages on the Modules screen are not all equal
        List<Lesson> firstChapter = algo.getChapters().getFirst().getLessons();
        firstChapter.forEach(lesson -> completions.save(new LessonCompletion(amira, lesson)));
        completions.save(new LessonCompletion(yacine, firstChapter.getFirst()));

        LOG.info("Dev seed: {} users and {} modules created. Log in as prof@eduflow.dz (admin) "
                + "or amira@eduflow.dz (student), password {}", users.count(), courses.count(), DEMO_PASSWORD);
    }

    private User admin(String fullName, String email) {
        return new User(fullName, email, passwordEncoder.encode(DEMO_PASSWORD), Role.ADMIN, null);
    }

    private User student(String fullName, String email, Level level) {
        return new User(fullName, email, passwordEncoder.encode(DEMO_PASSWORD), Role.STUDENT, level);
    }

    private static Course algorithmique() {
        Course course = new Course("INF201", "Algorithmique et programmation",
                "Concevoir, écrire et tester un algorithme.", Level.THIRD_AS, "#16A34A");

        Chapter bases = course.addChapter(new Chapter("Introduction à l'algorithmique",
                "Vocabulaire, variables et premiers algorithmes."), null);
        bases.addLesson(new Lesson("Variables et types", LessonType.VIDEO, 14), null);
        bases.addLesson(new Lesson("Opérateurs et expressions", LessonType.VIDEO, 11), null);
        bases.addLesson(new Lesson("Fiche d'exercices n°1", LessonType.PDF, null), null);

        Chapter controle = course.addChapter(new Chapter("Structures de contrôle",
                "Conditions et boucles."), null);
        controle.addLesson(new Lesson("Les conditions", LessonType.VIDEO, 18), null);
        controle.addLesson(new Lesson("Les boucles", LessonType.VIDEO, 22), null);
        controle.addLesson(new Lesson("Quiz — structures de contrôle", LessonType.QUIZ, 15), null);
        controle.addLesson(new Lesson("TP n°1 — Calculatrice", LessonType.TASK, null), null);

        Chapter tableaux = course.addChapter(new Chapter("Les tableaux", null), null);
        tableaux.addLesson(new Lesson("Déclaration et parcours", LessonType.VIDEO, 16), null);
        tableaux.addLesson(new Lesson("Recherche et tri", LessonType.VIDEO, 25), null);
        tableaux.addLesson(new Lesson("TP n°2 — Tri d'un tableau", LessonType.TASK, null), null);
        return course;
    }

    private static Course basesDeDonnees() {
        Course course = new Course("INF202", "Bases de données",
                "Du modèle conceptuel aux requêtes SQL.", Level.THIRD_AS, "#2563EB");

        Chapter modele = course.addChapter(new Chapter("Le modèle relationnel", null), null);
        modele.addLesson(new Lesson("Entités et associations", LessonType.VIDEO, 20), null);
        modele.addLesson(new Lesson("Du MCD au modèle relationnel", LessonType.PDF, null), null);

        Chapter sql = course.addChapter(new Chapter("SQL", "Interroger une base."), null);
        sql.addLesson(new Lesson("SELECT et filtres", LessonType.VIDEO, 17), null);
        sql.addLesson(new Lesson("Les jointures", LessonType.VIDEO, 21), null);
        sql.addLesson(new Lesson("Quiz — SQL", LessonType.QUIZ, 20), null);
        return course;
    }

    private static Course initiation() {
        Course course = new Course("INF101", "Initiation à l'informatique",
                "Découvrir l'ordinateur et ses usages.", Level.SECOND_AS, "#D97706");

        Chapter machine = course.addChapter(new Chapter("L'ordinateur", null), null);
        machine.addLesson(new Lesson("Matériel et logiciel", LessonType.VIDEO, 12), null);
        machine.addLesson(new Lesson("Les systèmes d'exploitation", LessonType.VIDEO, 15), null);

        Chapter internet = course.addChapter(new Chapter("Internet et sécurité", null), null);
        internet.addLesson(new Lesson("Naviguer en sécurité", LessonType.VIDEO, 13), null);
        internet.addLesson(new Lesson("Fiche — bonnes pratiques", LessonType.PDF, null), null);
        return course;
    }

    private static Course bureautique() {
        Course course = new Course("INF102", "Bureautique",
                "Traitement de texte et tableur.", Level.SECOND_AS, "#7C3AED");

        Chapter texte = course.addChapter(new Chapter("Traitement de texte", null), null);
        texte.addLesson(new Lesson("Mise en forme d'un document", LessonType.VIDEO, 10), null);
        texte.addLesson(new Lesson("TP — Rédiger un rapport", LessonType.TASK, null), null);
        return course;
    }
}
