package com.example.education_platform.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.course.entity.Chapter;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.entity.Lesson;
import com.example.education_platform.course.entity.LessonType;
import com.example.education_platform.user.entity.Level;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Positions must stay 1..n with no gaps, whatever order things are added, moved or removed in. */
class CourseOrderingTest {

    @Test
    void addingWithoutAPositionAppends() {
        Course course = course();
        course.addChapter(new Chapter("Premier", null), null);
        course.addChapter(new Chapter("Deuxième", null), null);

        assertThat(titlesOf(course)).containsExactly("Premier", "Deuxième");
        assertThat(positionsOf(course)).containsExactly(1, 2);
    }

    @Test
    void insertingAtAPositionShiftsTheRest() {
        Course course = course();
        course.addChapter(new Chapter("A", null), null);
        course.addChapter(new Chapter("B", null), null);

        course.addChapter(new Chapter("Intercalé", null), 2);

        assertThat(titlesOf(course)).containsExactly("A", "Intercalé", "B");
        assertThat(positionsOf(course)).containsExactly(1, 2, 3);
    }

    @Test
    void anOutOfRangePositionIsClampedInsteadOfThrowing() {
        Course course = course();
        course.addChapter(new Chapter("A", null), null);

        course.addChapter(new Chapter("Trop bas", null), -5);
        course.addChapter(new Chapter("Trop haut", null), 99);

        assertThat(titlesOf(course)).containsExactly("Trop bas", "A", "Trop haut");
        assertThat(positionsOf(course)).containsExactly(1, 2, 3);
    }

    @Test
    void movingAChapterRenumbersEverything() {
        Course course = course();
        Chapter first = course.addChapter(new Chapter("A", null), null);
        course.addChapter(new Chapter("B", null), null);
        course.addChapter(new Chapter("C", null), null);

        course.moveChapter(first, 3);

        assertThat(titlesOf(course)).containsExactly("B", "C", "A");
        assertThat(positionsOf(course)).containsExactly(1, 2, 3);
    }

    @Test
    void removingAChapterClosesTheGap() {
        Course course = course();
        course.addChapter(new Chapter("A", null), null);
        Chapter middle = course.addChapter(new Chapter("B", null), null);
        course.addChapter(new Chapter("C", null), null);

        course.removeChapter(middle);

        assertThat(titlesOf(course)).containsExactly("A", "C");
        assertThat(positionsOf(course)).containsExactly(1, 2);
    }

    @Test
    void lessonsInsideAChapterAreOrderedTheSameWay() {
        Chapter chapter = new Chapter("Chapitre", null);
        chapter.addLesson(new Lesson("Vidéo", LessonType.VIDEO, 12), null);
        Lesson pdf = chapter.addLesson(new Lesson("PDF", LessonType.PDF, null), null);
        chapter.addLesson(new Lesson("TP", LessonType.TASK, null), 1);

        assertThat(chapter.getLessons().stream().map(Lesson::getTitle))
                .containsExactly("TP", "Vidéo", "PDF");

        chapter.removeLesson(pdf);
        assertThat(chapter.getLessons().stream().map(Lesson::getPosition)).containsExactly(1, 2);
    }

    @Test
    void lessonCountSpansEveryChapter() {
        Course course = course();
        Chapter one = course.addChapter(new Chapter("Un", null), null);
        Chapter two = course.addChapter(new Chapter("Deux", null), null);
        one.addLesson(new Lesson("L1", LessonType.VIDEO, 5), null);
        one.addLesson(new Lesson("L2", LessonType.PDF, null), null);
        two.addLesson(new Lesson("L3", LessonType.QUIZ, null), null);

        assertThat(course.lessonCount()).isEqualTo(3);
    }

    private static Course course() {
        return new Course("INF201", "Algorithmique", "Bases", Level.THIRD_AS, "#16A34A");
    }

    private static List<String> titlesOf(Course course) {
        return course.getChapters().stream().map(Chapter::getTitle).toList();
    }

    private static List<Integer> positionsOf(Course course) {
        return course.getChapters().stream().map(Chapter::getPosition).toList();
    }
}
