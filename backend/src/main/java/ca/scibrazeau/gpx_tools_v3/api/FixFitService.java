package ca.scibrazeau.gpx_tools_v3.api;


import ca.scibrazeau.gpx_tools.misc.BrytonFitFixerMin;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;


@RestController()
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/utils")
public class FixFitService {


    @RequestMapping(value="/fitfix", method=RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> fixFit(@RequestParam("file") MultipartFile file) throws Exception {
        File fitFile = File.createTempFile("tmp", ".fit");
        file.transferTo(fitFile);
        fitFile.deleteOnExit();
        BrytonFitFixerMin.main(new String[] {fitFile.getAbsolutePath()});
        fitFile.delete();
        File fixedFitFileAsGpx = new File(FilenameUtils.removeExtension(fitFile.getAbsolutePath()) + ".gpx");
        FileSystemResource toReturn = new FileSystemResource(fixedFitFileAsGpx);
        HttpHeaders headers = new HttpHeaders();
        String filename = FilenameUtils.removeExtension(file.getOriginalFilename()) + ".gpx";
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData(filename, filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        byte[] contents;
        try (
            FileInputStream fis = new FileInputStream(fixedFitFileAsGpx);
        ) {
            contents = IOUtils.toByteArray(fis);
        }
        ResponseEntity<byte[]> response = new ResponseEntity<>(contents, headers, HttpStatus.OK);
        return response;
    }


}
