package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "student-1";
    private static final String COURSE_ID = "CS101";
    private static final String EMAIL = "student@example.edu";
    private static final String SUBJECT = "Бүртгэл баталгаажлаа";

    @Mock
    StudentRepository repository;

    @Mock
    EmailSender emailSender;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    @Test
    void register_activeStudent_returnsOkAndSavesAndSendsEmail() {
        givenEligibleStudent();

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), eq(SUBJECT), contains(COURSE_ID));
    }

    @Test
    void register_missingStudent_returnsNoStudentErrorWithoutSavingOrEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void register_inactiveStudent_returnsInactiveErrorWithoutEmailInteraction() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(false, List.of()));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
    }

    @Test
    void register_missingPrerequisite_returnsPrerequisitesErrorWithoutSaving() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS100"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void register_emailSenderThrows_keepsRegistrationSuccessful() {
        givenEligibleStudent();
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailSender).send(eq(EMAIL), eq(SUBJECT), contains(COURSE_ID));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), eq(SUBJECT), contains(COURSE_ID));
    }

    @Test
    void register_nullInput_throwsIllegalArgumentExceptionWithoutDependencyInteractions() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));

        verifyNoInteractions(repository, emailSender);
    }

    @Test
    void register_successfulRegistration_includesCourseCodeInEmailBody() {
        givenEligibleStudent();
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        service.register(STUDENT_ID, COURSE_ID);

        verify(emailSender).send(eq(EMAIL), eq(SUBJECT), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains(COURSE_ID));
    }

    @ParameterizedTest
    @CsvSource({
            "NO_STUDENT, ERROR_NO_STUDENT",
            "INACTIVE_STUDENT, ERROR_INACTIVE_STUDENT",
            "NO_COURSE, ERROR_NO_COURSE"
    })
    void register_errorScenario_returnsExpectedErrorWithoutSaving(String scenario, String expectedResult) {
        switch (scenario) {
            case "NO_STUDENT" -> when(repository.findStudent(STUDENT_ID)).thenReturn(null);
            case "INACTIVE_STUDENT" -> when(repository.findStudent(STUDENT_ID))
                    .thenReturn(student(false, List.of()));
            case "NO_COURSE" -> {
                when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of()));
                when(repository.courseExists(COURSE_ID)).thenReturn(false);
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expectedResult, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    private void givenEligibleStudent() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of("CS100")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS100"));
    }

    private Student student(boolean active, List<String> coursesTaken) {
        return new Student(STUDENT_ID, EMAIL, active, coursesTaken);
    }
}
