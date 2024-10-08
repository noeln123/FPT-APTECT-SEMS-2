package com.example.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.Course;
import com.example.demo.entity.Lecture;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.LectureRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private LectureRepository lectureRepository;

    @Value("${upload_course.path}")
    private String uploadPath;

    public Course getCourseById(Integer id) {
        return courseRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));
    }

    public List<Course> getAllCourse() {
        return courseRepository.findAllByState("APPROVED");
    }

    public Course createCourse(String title, String description, Double price, MultipartFile image){
        // Lưu file ảnh trên server
        String imageName = saveImage(image);
        
        
        
        Course newCourse = new Course();
        newCourse.setTitle(title);
        newCourse.setDescription(description);
        newCourse.setPrice(price);
        newCourse.setImg(imageName);

        newCourse.setTeacherId(userService.getMyinfo().getId());
        newCourse.setState("PENDING");
        return courseRepository.save(newCourse);
    }

    public String updateCourse(Integer id, Course course){
        // Kiểm tra xem người dùng hiện tại có quyền chỉnh sửa khóa học này hay không
        if (userService.getMyinfo().getId() != getCourseById(id).getTeacherId() && hasScopeAdmin() == false){
            throw new AppException(ErrorCode.PERMISSION_COURSE_DENIED);
        }

        Course course2 = getCourseById(id);

        course2.setDescription(course.getDescription());
        course2.setPrice(course.getPrice());
        course2.setTitle(course.getTitle());

        courseRepository.save(course2);

        return("Update course information successfully!");
    }

    public void deleteCourse(Integer id){
        // Kiểm tra xem người dùng hiện tại có quyền chỉnh sửa khóa học này hay không
        if (userService.getMyinfo().getId() != getCourseById(id).getTeacherId() && hasScopeAdmin() == false){
            throw new AppException(ErrorCode.PERMISSION_COURSE_DENIED);
        }
        courseRepository.deleteById(id);
    }


    public boolean hasScopeAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    for (GrantedAuthority authority : authentication.getAuthorities()) {
        if (authority.getAuthority().equals("SCOPE_ADMIN")) {
            return true;
        }
    
    }return false;};

    
    public List<Course> getallCoursesById(){
        return courseRepository.findAllByTeacherId(userService.getMyinfo().getId());
    }

    public Lecture getLectureById(Integer id) {
        return lectureRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.LECTURE_NOT_EXISTED));
    }

    public Lecture addLecture(Integer courseId, Lecture lecture){
        if (userService.getMyinfo().getId() != getCourseById(courseId).getTeacherId() && hasScopeAdmin() == false){
            throw new AppException(ErrorCode.PERMISSION_COURSE_DENIED);
        }
        lecture.setCourseId(courseId);
        lecture.setState("PENDING");
        return lectureRepository.save(lecture);
    }

    public Lecture updateLecture(Integer lectureId ,Lecture lecture){
        if(!lectureRepository.existsById(lectureId)){
            throw new AppException(ErrorCode.LECTURE_NOT_EXISTED);
        }
        if (userService.getMyinfo().getId() != getCourseById(getLectureById(lectureId).getCourseId()).getTeacherId() && hasScopeAdmin() == false){
            throw new AppException(ErrorCode.PERMISSION_COURSE_DENIED);
        }

        Lecture lectureUpdate = lectureRepository.findById(lectureId).get();
        lectureUpdate.setContent(lecture.getContent());
        lectureUpdate.setTitle(lecture.getTitle());

        return lectureRepository.save(lectureUpdate);
    } 

    public void deleteLecture(Integer id){
        // Kiểm tra xem người dùng hiện tại có quyền chỉnh sửa khóa học này hay không
        if (userService.getMyinfo().getId() != getCourseById(getLectureById(id).getCourseId()).getTeacherId() && hasScopeAdmin() == false){
            throw new AppException(ErrorCode.LECTURE_NOT_EXISTED);
        }
        lectureRepository.deleteById(id);
    }


    public Integer getMyNewestCourseId(){
        return courseRepository.findLatestCourseIdByTeacherId(userService.getMyinfo().getId()).get().get(0);
    }



    // Hàm lưu file ảnh và trả về tên file
    private String saveImage(MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            String originalFilename = StringUtils.cleanPath(image.getOriginalFilename());
            String extension = StringUtils.getFilenameExtension(originalFilename);
            String newFilename = UUID.randomUUID().toString() + "." + extension; // Đặt tên file ngẫu nhiên

            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            try {
                Files.copy(image.getInputStream(), Paths.get(uploadPath + newFilename));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return newFilename;
        }
        return null;
    }


    public void acceptCourse(Integer id){
        Course course = getCourseById(id);
        course.setState("APPROVED");
        courseRepository.save(course);
    }

    public void rejectCourse(Integer id, String reason){
        Course course = getCourseById(id);
        course.setReject_reason(reason);
        course.setState("REJECTED");
        courseRepository.save(course);
    }

    public void rejectLecture(Integer id, String reason){
        Lecture lecture = getLectureById(id);
        lecture.setReject_reason(reason);
        lecture.setState("REJECTED");
        lectureRepository.save(lecture);
    }

    public String acceptLecture(Integer id){
        Lecture lecture = getLectureById(id);
        lecture.setState("APPROVED");

        //Chuyển video từ file pending(private) sang uploads/video(public)
        String videoName = lecture.getVideo();

        File pendingDir = new File("pending/video");
        File videoFilePath = new File(pendingDir,videoName); // kết hợp đường dẫn

        File uploadDir = new File("uploads/video");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs(); // Tạo tất cả các thư mục con nếu cần
        }
        File newVideoFilePath = new File(uploadDir, videoName);

        try {
            // Di chuyển tệp video sang thư mục uploads/videos
            Files.move(videoFilePath.toPath(), newVideoFilePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to move the video file: " + e.getMessage());
            return "Failed to move the video file: " + e.getMessage();
        }

        lectureRepository.save(lecture);
        return "Success";
    }

    public List<Lecture> getAllLectureByCourseId(Integer id){
        return lectureRepository.findAllByCourseId(id);
    }
    
}
