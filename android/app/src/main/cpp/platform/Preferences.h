#pragma once
#include <cstddef>
#include <cstdint>
#include <map>
#include <string>
#include <vector>
class Preferences {
public:
 bool begin(const char*, bool=false); void end(); bool clear(); bool isKey(const char*); bool remove(const char*);
 size_t putBool(const char*,bool); size_t putUChar(const char*,uint8_t); size_t putChar(const char*,int8_t);
 size_t putShort(const char*,int16_t); size_t putUShort(const char*,uint16_t); size_t putInt(const char*,int32_t);
 size_t putUInt(const char*,uint32_t); size_t putBytes(const char*,const void*,size_t); size_t putString(const char*,const char*);
 bool getBool(const char*,bool=false); uint8_t getUChar(const char*,uint8_t=0); int8_t getChar(const char*,int8_t=0);
 int16_t getShort(const char*,int16_t=0); uint16_t getUShort(const char*,uint16_t=0); int32_t getInt(const char*,int32_t=0);
 uint32_t getUInt(const char*,uint32_t=0); size_t getBytes(const char*,void*,size_t); size_t getString(const char*,char*,size_t);
 static void setStorageDirectory(const char*);
private:
 std::string ns_; bool open_=false, ro_=false; std::map<std::string,std::vector<uint8_t>> values_;
 std::string filePath() const; bool load(); bool persist() const;
 template<typename T> size_t putNumber(const char*,T); template<typename T> T getNumber(const char*,T);
};
