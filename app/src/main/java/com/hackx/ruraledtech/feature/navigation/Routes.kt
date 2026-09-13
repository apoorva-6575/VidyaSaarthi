package com.hackx.ruraledtech.feature.navigation

object Routes {
    const val SPLASH = "splash"
    const val ROLE_SELECTION = "onboarding/role"
    const val TEACHER_LOGIN = "teacher_login"
    const val TEACHER_HOME = "teacher_home"
    const val CLASS_ANALYTICS = "teacher/classes/{classId}/analytics"
    const val LANGUAGE_SELECTION = "onboarding/language"
    const val ADD_LEARNER = "onboarding/add_learner"
    const val LEARNER_SELECTION = "learner_selection"
    const val HOME = "home"
    const val SUBJECT_LIST = "subjects"
    const val LESSON_LIST = "subjects/{subject}/lessons"
    const val LESSON_VIEWER = "lessons/{lessonId}"
    const val QUIZ = "lessons/{lessonId}/quiz"
    const val PROGRESS = "progress"
    const val CONCEPT_DETAIL = "progress/{conceptId}"
    const val PROFILE = "profile"
    const val CONTENT_LIBRARY = "content_library"
    const val ACCESSIBILITY_SETTINGS = "settings/accessibility"
    const val PASSPORT = "passport"

    fun lessonList(subject: String) = "subjects/$subject/lessons"
    fun lessonViewer(lessonId: String) = "lessons/$lessonId"
    fun quiz(lessonId: String) = "lessons/$lessonId/quiz"
    fun conceptDetail(conceptId: String) = "progress/$conceptId"

    const val ARG_SUBJECT = "subject"
    const val ARG_LESSON_ID = "lessonId"
    const val ARG_CONCEPT_ID = "conceptId"
    const val ARG_CLASS_ID = "classId"
}
