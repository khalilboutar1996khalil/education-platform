package com.example.education_platform.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.education_platform.common.config.JpaAuditingConfig;
import com.example.education_platform.course.entity.Chapter;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.entity.Lesson;
import com.example.education_platform.course.entity.LessonCompletion;
import com.example.education_platform.course.entity.LessonType;
import com.example.education_platform.course.repository.CourseCount;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.course.repository.LessonCompletionRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courses;

    @Autowired
    private LessonCompletionRepository completions;

    @Autowired
    private UserRepository users;

    private Course algo;
    private User amira;

    @BeforeEach
    void seedCurriculum() {
        algo = new Course("INF201", "Algorithmique", "Bases", Level.THIRD_AS, "#16A34A");
        Chapter intro = algo.addChapter(new Chapter("Introduction", "Les bases"), null);
        intro.addLesson(new Lesson("Variables", LessonType.VIDEO, 12), null);
        intro.addLesson(new Lesson("Exercices", LessonType.PDF, null), null);
        Chapter boucles = algo.addChapter(new Chapter("Boucles", null), null);
        boucles.addLesson(new Lesson("Pour / Tant que", LessonType.VIDEO, 20), null);
        courses.saveAndFlush(algo);

        courses.saveAndFlush(new Course("INF101", "Initiation", null, Level.SECOND_AS, "#2563EB"));

        amira = users.saveAndFlush(new User("Amira Benali", "amira@eduflow.dz",
                "hash", Role.STUDENT, Level.THIRD_AS));
    }

    @Test
    void savingACourseCascadesItsChaptersAndLessons() {
        Course reloaded = courses.findDetailById(algo.getId()).orElseThrow();

        assertThat(reloaded.getChapters()).hasSize(2);
        assertThat(reloaded.getChapters().getFirst().getLessons()).hasSize(2);
        assertThat(reloaded.lessonCount()).isEqualTo(3);
        assertThat(reloaded.getCreatedBy()).isEqualTo("system");
    }

    @Test
    void chaptersAndLessonsComeBackInPositionOrder() {
        Course reloaded = courses.findDetailById(algo.getId()).orElseThrow();

        assertThat(reloaded.getChapters().stream().map(Chapter::getTitle))
                .containsExactly("Introduction", "Boucles");
        assertThat(reloaded.getChapters().getFirst().getLessons().stream().map(Lesson::getTitle))
                .containsExactly("Variables", "Exercices");
    }

    @Test
    void removingAChapterDeletesItsLessonsThroughOrphanRemoval() {
        Course reloaded = courses.findDetailById(algo.getId()).orElseThrow();
        reloaded.removeChapter(reloaded.getChapters().getFirst());
        courses.saveAndFlush(reloaded);

        Course afterDelete = courses.findDetailById(algo.getId()).orElseThrow();
        assertThat(afterDelete.getChapters()).hasSize(1);
        assertThat(afterDelete.lessonCount()).isEqualTo(1);
    }

    @Test
    void aDuplicateCodeIsRejected() {
        assertThat(courses.existsByCodeIgnoreCase("inf201")).isTrue();
        assertThat(courses.existsByCodeIgnoreCaseAndIdNot("INF201", algo.getId())).isFalse();

        assertThatThrownBy(() -> courses.saveAndFlush(
                new Course("INF201", "Doublon", null, Level.SECOND_AS, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findVisibleFiltersByLevelAndANullLevelShowsEverything() {
        var byCode = PageRequest.of(0, 10, Sort.by("code"));

        assertThat(courses.findVisible(Level.THIRD_AS, byCode).getContent())
                .extracting(Course::getCode).containsExactly("INF201");
        assertThat(courses.findVisible(Level.SECOND_AS, byCode).getContent())
                .extracting(Course::getCode).containsExactly("INF101");
        assertThat(courses.findVisible(null, byCode).getContent())
                .extracting(Course::getCode).containsExactly("INF101", "INF201");
    }

    @Test
    void countsPerCourseAreOneQueryEach() {
        assertThat(totals(courses.countChaptersPerCourse())).containsEntry(algo.getId(), 2L);
        assertThat(totals(courses.countLessonsPerCourse())).containsEntry(algo.getId(), 3L);
    }

    @Test
    void completingALessonIsIdempotentPerStudent() {
        Lesson lesson = firstLesson();
        completions.saveAndFlush(new LessonCompletion(amira, lesson));

        assertThat(completions.existsByStudentIdAndLessonId(amira.getId(), lesson.getId())).isTrue();
        assertThatThrownBy(() -> completions.saveAndFlush(new LessonCompletion(amira, lesson)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void completionsDriveTheProgressQueries() {
        Course reloaded = courses.findDetailById(algo.getId()).orElseThrow();
        List<Lesson> lessons = reloaded.getChapters().getFirst().getLessons();
        completions.saveAndFlush(new LessonCompletion(amira, lessons.get(0)));
        completions.saveAndFlush(new LessonCompletion(amira, lessons.get(1)));

        assertThat(completions.findCompletedLessonIds(amira.getId(), algo.getId()))
                .containsExactlyInAnyOrder(lessons.get(0).getId(), lessons.get(1).getId());
        assertThat(totals(completions.countPerCourseForStudent(amira.getId())))
                .containsEntry(algo.getId(), 2L);
        assertThat(totals(completions.countPerCourse())).containsEntry(algo.getId(), 2L);
    }

    @Test
    void unmarkingALessonRemovesTheCompletion() {
        Lesson lesson = firstLesson();
        completions.saveAndFlush(new LessonCompletion(amira, lesson));

        assertThat(completions.deleteByStudentIdAndLessonId(amira.getId(), lesson.getId())).isEqualTo(1);
        assertThat(completions.existsByStudentIdAndLessonId(amira.getId(), lesson.getId())).isFalse();
    }

    private Lesson firstLesson() {
        return courses.findDetailById(algo.getId()).orElseThrow()
                .getChapters().getFirst().getLessons().getFirst();
    }

    private static Map<Long, Long> totals(List<CourseCount> counts) {
        return counts.stream().collect(Collectors.toMap(CourseCount::getCourseId, CourseCount::getTotal));
    }
}
