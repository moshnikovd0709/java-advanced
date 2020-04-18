package ru.ifmo.rain.moshnikov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements info.kgeorgiy.java.advanced.student.StudentGroupQuery {

    private static final Comparator<Student> STUDENT_COMPARATOR = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparingInt(Student::getId);




    // Realization of StudentQuery //

    //realization of map, that give required information about Student
    private List<String> studentFunction(List<Student> students, Function<Student, String> function) {
        return students.stream().map(function).collect(Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return studentFunction(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return studentFunction(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return studentFunction(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return studentFunction(students, Student::getGroup);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new HashSet<>(studentFunction(students, Student::getFirstName));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }


    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted(Comparator.comparing(Student::getId)).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream().sorted(STUDENT_COMPARATOR).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String FirstName) {
        return students.stream().sorted(STUDENT_COMPARATOR).filter(student -> student.getFirstName().equals(FirstName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String LastName) {
        return students.stream().sorted(STUDENT_COMPARATOR).filter(student -> student.getLastName().equals(LastName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return students.stream().sorted(STUDENT_COMPARATOR).filter(student -> student.getGroup().equals(group))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream()
                .filter(student -> student.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }


    // Realization of StudentGroupQuery //

    //return groups of sorted students
    private List<Group> getListGroup(Stream<Map.Entry<String, List<Student>>> groups, UnaryOperator<List<Student>> students) {
        return groups.map(g -> new Group(g.getKey(), students.apply(g.getValue()))).collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getListGroup(students.stream().collect(Collectors.groupingBy(Student::getGroup,
                TreeMap::new, Collectors.toList())).entrySet().stream(), this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getListGroup(students.stream().collect(Collectors.groupingBy(Student::getGroup,
                TreeMap::new, Collectors.toList())).entrySet().stream(), this::sortStudentsById);
    }

    //return max element in the group by some filter
    private String getMaxGroupElement(Stream<Map.Entry<String, List<Student>>> groups, ToIntFunction<List<Student>> filter) {
        return groups.max(Comparator.comparingInt((Map.Entry<String, List<Student>> g) -> filter.applyAsInt(g.getValue()))
                .thenComparing(Map.Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Map.Entry::getKey).orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getMaxGroupElement(students.stream().collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList())).entrySet().stream(),
                List::size);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getMaxGroupElement(students.stream().collect(Collectors.groupingBy(Student::getGroup, TreeMap::new, Collectors.toList())).entrySet().stream()
                , s -> getDistinctFirstNames(s).size());
    }

}