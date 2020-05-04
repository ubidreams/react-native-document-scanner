Pod::Spec.new do |s|
    s.name         = "react-native-document-scanner"
    s.version      = "1.0.7"
    s.summary      = "Document scanner for React-Native"
    s.description  = "Document scanner with border detection, perspective correction and custom crop/resize"
    s.homepage     = "https://github.com/ubidreams/react-native-document-scanner"
    s.license      = "MIT"
    s.author       = "Romain MARQUOIS"
    s.platform     = :ios, "9.0"
    s.source       = { :git => "https://github.com/ubidreams/react-native-document-scanner", :tag => "master" }
    s.source_files  = "ios/**/*.{h,mm,m}"
    s.requires_arc = true
    s.static_framework = true
  
    s.dependency "libopencv-contrib", "~> 3.4.1"
    s.dependency "React"
  end
