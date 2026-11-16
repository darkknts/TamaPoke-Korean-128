#define UNICODE
#define _UNICODE
#include <windows.h>
#include <windowsx.h>
#include <ctime>
#include <string>
#include <cstdlib>
#include <fstream>
#include <vector>
#include <filesystem>
#include <algorithm>
#include <cstring>
#include <iterator>
#include <map>
#include <cmath>
#include "audio.h"
#include "Arduino.h"
#include "pet.h"
#include "dex.h"
#include "species.h"

namespace {
constexpr wchar_t kClassName[] = L"TamaPokeDesktopWindow";
constexpr int W=720, H=860;
constexpr wchar_t kMiniClassName[] = L"TamaPokeMiniPetWindow";
constexpr COLORREF kMiniTransparent = RGB(255,0,255);
HWND miniHwnd=nullptr;
HWND mainHwnd=nullptr;
bool miniMode=false;
Pet pet;
int page=0;
int dexPage=0;
bool gameActive=false; DWORD gameStarted=0; int gameScore=0; int targetX=360,targetY=330;
time_t lastClockSync=0;
int transientAction=0; DWORD transientActionUntil=0;
struct DesktopAnimation {
  int w=0,h=0;
  std::vector<uint16_t> pixels;
  std::vector<uint8_t> opaque;
  std::vector<uint16_t> durations;
  std::vector<HBITMAP> frameBitmaps;
  uint32_t totalDuration=0;
};
struct DesktopSprite { std::vector<DesktopAnimation> actions=std::vector<DesktopAnimation>(12); };
std::vector<DesktopSprite> desktopSprites(152);
COLORREF rgb565(uint16_t c) {
  int r=(c>>11)&31, g=(c>>5)&63, b=c&31;
  return RGB(r*255/31,g*255/63,b*255/31);
}
void buildAnimationBitmaps(DesktopAnimation& anim) {
  if(anim.w<=0||anim.h<=0||anim.durations.empty())return;
  const size_t frameBytes=(size_t)anim.w*anim.h;
  BITMAPINFO info{};
  info.bmiHeader.biSize=sizeof(BITMAPINFOHEADER);
  info.bmiHeader.biWidth=anim.w;
  info.bmiHeader.biHeight=-anim.h; // top-down DIB
  info.bmiHeader.biPlanes=1;
  info.bmiHeader.biBitCount=32;
  info.bmiHeader.biCompression=BI_RGB;
  anim.frameBitmaps.reserve(anim.durations.size());
  for(size_t frame=0;frame<anim.durations.size();++frame) {
    void* bits=nullptr;
    HBITMAP bitmap=CreateDIBSection(nullptr,&info,DIB_RGB_COLORS,&bits,nullptr,0);
    if(!bitmap||!bits) { if(bitmap)DeleteObject(bitmap); anim.frameBitmaps.push_back(nullptr); continue; }
    auto* dst=(uint32_t*)bits;
    for(size_t i=0;i<frameBytes;i++) {
      size_t src=frame*frameBytes+i;
      COLORREF c=rgb565(anim.pixels[src]);
      uint8_t a=anim.opaque[src]?255:0;
      dst[i]=((uint32_t)a<<24)|((uint32_t)GetRValue(c)<<16)|((uint32_t)GetGValue(c)<<8)|GetBValue(c);
    }
    anim.frameBitmaps.push_back(bitmap);
  }
}
void destroyDesktopBitmaps() {
  for(auto& sprite:desktopSprites)for(auto& anim:sprite.actions)
    for(HBITMAP bitmap:anim.frameBitmaps)if(bitmap)DeleteObject(bitmap);
}
uint16_t read16(const std::vector<unsigned char>& b,size_t p){return (uint16_t)(b[p]|(b[p+1]<<8));}
uint32_t read32(const std::vector<unsigned char>& b,size_t p){return (uint32_t)b[p]|((uint32_t)b[p+1]<<8)|((uint32_t)b[p+2]<<16)|((uint32_t)b[p+3]<<24);}
void loadDesktopSprites() {
  wchar_t exe[MAX_PATH]{}; GetModuleFileNameW(nullptr,exe,MAX_PATH);
  std::filesystem::path packPath(exe); packPath=packPath.parent_path()/L"sprites.pak";
  std::ifstream in(packPath,std::ios::binary); if(!in)return;
  std::vector<unsigned char> b((std::istreambuf_iterator<char>(in)),{});
  if(b.size()<6||memcmp(b.data(),"TPAK",4)!=0)return;
  uint16_t count=read16(b,4); size_t p=6;
  struct Entry{std::string name;uint32_t size;};std::vector<Entry> entries;entries.reserve(count);
  for(uint16_t i=0;i<count;i++){
    if(p>=b.size())return;uint8_t n=b[p++];if(p+n+4>b.size())return;
    std::string name((char*)b.data()+p,n);p+=n;uint32_t size=read32(b,p);p+=4;entries.push_back({name,size});
  }
  for(const auto& e:entries){
    if(p+e.size>b.size())return;
    if(e.name.rfind("mons/p",0)==0&&e.name.size()==13&&e.name.substr(9)==".bin"){
      int dex=std::atoi(e.name.substr(6,3).c_str());
      if(dex>=1&&dex<=151&&e.size>=7&&memcmp(b.data()+p,"TPK2",4)==0){
        size_t q=p;uint8_t acts=b[q+4];uint16_t colors=read16(b,q+5);q+=7;
        if(q+(size_t)colors*2<=p+e.size){
          std::vector<uint16_t> pal(colors);for(uint16_t k=0;k<colors;k++)pal[k]=read16(b,q+k*2);q+=(size_t)colors*2;
          for(uint8_t a=0;a<acts&&q+4<=p+e.size;a++){
            uint8_t id=b[q],w=b[q+1],h=b[q+2],frames=b[q+3];q+=4;
            if(!w||!h||!frames||q+(size_t)frames*2+(size_t)w*h*frames>p+e.size)break;
            size_t frameBytes=(size_t)w*h;
            size_t durationBytes=(size_t)frames*2;
            size_t pixelBytes=frameBytes*frames;
            if(id==0||id==1||id==2||id==3||id==4||id==6||id==7||id==8||id==9||id==10||id==11){
              DesktopAnimation anim;anim.w=w;anim.h=h;
              anim.durations.reserve(frames);
              for(uint8_t frame=0;frame<frames;frame++){
                uint16_t duration=read16(b,q+(size_t)frame*2);
                anim.durations.push_back(duration?duration:100);
                anim.totalDuration+=anim.durations.back();
              }
              q+=durationBytes;
              anim.pixels.resize(pixelBytes);anim.opaque.resize(pixelBytes);
              for(size_t z=0;z<pixelBytes;z++){
                uint8_t ix=b[q+z];
                anim.opaque[z]=(ix!=255&&ix<pal.size())?1:0;
                anim.pixels[z]=anim.opaque[z]?pal[ix]:0;
              }
              buildAnimationBitmaps(anim);
              anim.pixels.clear(); anim.pixels.shrink_to_fit();
              anim.opaque.clear(); anim.opaque.shrink_to_fit();
              desktopSprites[dex].actions[id]=std::move(anim);
            }
            q+=pixelBytes;
          }
        }
      }
    }
    p+=e.size;
  }
}
std::wstring notice=L"반가워요! 타마포크를 시작해 보세요.";

void drawDesktopSprite(HDC dc,int dex,RECT bounds,int actionId=0,DWORD animationClock=0,BYTE opacity=255) {
  if(dex<1||dex>151)return;
  const auto& sprite=desktopSprites[dex];
  if(actionId<0||actionId>=12)actionId=0;
  const DesktopAnimation* anim=&sprite.actions[actionId];
  if(anim->w<=0||anim->h<=0||anim->durations.empty())anim=&sprite.actions[0];
  const auto& sp=*anim;
  if(sp.w<=0||sp.h<=0||sp.durations.empty()||sp.frameBitmaps.size()!=sp.durations.size())return;
  size_t frame=0;
  if(sp.totalDuration){uint32_t tick=animationClock%sp.totalDuration;while(frame+1<sp.durations.size()&&tick>=sp.durations[frame]){tick-=sp.durations[frame];frame++;}}
  const int bw=bounds.right-bounds.left,bh=bounds.bottom-bounds.top;
  if(bw<=0||bh<=0)return;
  const double scale=std::min((double)bw/sp.w,(double)bh/sp.h);
  const int dw=std::max(1,(int)(sp.w*scale)),dh=std::max(1,(int)(sp.h*scale));
  if(opacity==0||frame>=sp.frameBitmaps.size()||!sp.frameBitmaps[frame])return;
  const int ox=bounds.left+(bw-dw)/2,oy=bounds.top+(bh-dh)/2;
  HDC memory=CreateCompatibleDC(dc);
  HGDIOBJ previous=SelectObject(memory,sp.frameBitmaps[frame]);
  BLENDFUNCTION blend{AC_SRC_OVER,0,opacity,AC_SRC_ALPHA};
  AlphaBlend(dc,ox,oy,dw,dh,memory,0,0,sp.w,sp.h,blend);
  SelectObject(memory,previous);
  DeleteDC(memory);
}
void text(HDC dc,const std::wstring&s,RECT r,int size,COLORREF color,bool bold=false) {
  HFONT f=CreateFontW(size,0,0,0,bold?FW_BOLD:FW_NORMAL,FALSE,FALSE,FALSE,
    DEFAULT_CHARSET,OUT_DEFAULT_PRECIS,CLIP_DEFAULT_PRECIS,CLEARTYPE_QUALITY,
    DEFAULT_PITCH,L"Malgun Gothic");
  auto old=SelectObject(dc,f); SetBkMode(dc,TRANSPARENT); SetTextColor(dc,color);
  DrawTextW(dc,s.c_str(),-1,&r,DT_CENTER|DT_VCENTER|DT_SINGLELINE|DT_END_ELLIPSIS);
  SelectObject(dc,old); DeleteObject(f);
}
void fillRound(HDC dc,RECT r,COLORREF fill,COLORREF edge,int radius=18) {
  HBRUSH b=CreateSolidBrush(fill); HPEN p=CreatePen(PS_SOLID,2,edge);
  auto ob=SelectObject(dc,b); auto op=SelectObject(dc,p);
  RoundRect(dc,r.left,r.top,r.right,r.bottom,radius,radius);
  SelectObject(dc,ob); SelectObject(dc,op); DeleteObject(b); DeleteObject(p);
}
void button(HDC dc,int x,int y,int w,int h,const wchar_t* label,bool active=false) {
  fillRound(dc,RECT{x,y,x+w,y+h},active?RGB(255,224,170):RGB(255,250,241),
            active?RGB(205,135,65):RGB(218,198,169),16);
  text(dc,label,RECT{x+4,y+2,x+w-4,y+h-2},18,RGB(82,64,51),true);
}
void bar(HDC dc,int x,int y,const wchar_t* label,uint8_t value,COLORREF tint) {
  text(dc,label,RECT{x,y,x+90,y+26},15,RGB(83,72,62),true);
  RECT track{x+92,y+5,x+380,y+23}; fillRound(dc,track,RGB(226,218,204),RGB(226,218,204),10);
  int width=(track.right-track.left)*value/100;
  if(width>0){RECT f{track.left,track.top,track.left+width,track.bottom};fillRound(dc,f,tint,tint,10);}
  text(dc,std::to_wstring(value),RECT{x+390,y,x+435,y+26},14,RGB(83,72,62),true);
}
std::wstring fromUtf8(const char* value) {
  if (!value || !*value) return L"";
  int n=MultiByteToWideChar(CP_UTF8,0,value,-1,nullptr,0);
  if(n<=1) return L"";
  std::wstring out(n,L'\\0'); MultiByteToWideChar(CP_UTF8,0,value,-1,out.data(),n); out.resize(n-1);
  return out;
}
static const wchar_t* const KOREAN_DEX_NAMES[DEX_COUNT + 1] = {
  L"미등록",
  L"이상해씨", // 1
  L"이상해풀", // 2
  L"이상해꽃", // 3
  L"파이리", // 4
  L"리자드", // 5
  L"리자몽", // 6
  L"꼬부기", // 7
  L"어니부기", // 8
  L"거북왕", // 9
  L"캐터피", // 10
  L"단데기", // 11
  L"버터플", // 12
  L"뿔충이", // 13
  L"딱충이", // 14
  L"독침붕", // 15
  L"구구", // 16
  L"피죤", // 17
  L"피죤투", // 18
  L"꼬렛", // 19
  L"레트라", // 20
  L"깨비참", // 21
  L"깨비드릴조", // 22
  L"아보", // 23
  L"아보크", // 24
  L"피카츄", // 25
  L"라이츄", // 26
  L"모래두지", // 27
  L"고지", // 28
  L"니드런♀", // 29
  L"니드리나", // 30
  L"니드퀸", // 31
  L"니드런♂", // 32
  L"니드리노", // 33
  L"니드킹", // 34
  L"삐삐", // 35
  L"픽시", // 36
  L"식스테일", // 37
  L"나인테일", // 38
  L"푸린", // 39
  L"푸크린", // 40
  L"주뱃", // 41
  L"골뱃", // 42
  L"뚜벅쵸", // 43
  L"냄새꼬", // 44
  L"라플레시아", // 45
  L"파라스", // 46
  L"파라섹트", // 47
  L"콘팡", // 48
  L"도나리", // 49
  L"디그다", // 50
  L"닥트리오", // 51
  L"나옹", // 52
  L"페르시온", // 53
  L"고라파덕", // 54
  L"골덕", // 55
  L"망키", // 56
  L"성원숭", // 57
  L"가디", // 58
  L"윈디", // 59
  L"발챙이", // 60
  L"슈륙챙이", // 61
  L"강챙이", // 62
  L"캐이시", // 63
  L"윤겔라", // 64
  L"후딘", // 65
  L"알통몬", // 66
  L"근육몬", // 67
  L"괴력몬", // 68
  L"모다피", // 69
  L"우츠동", // 70
  L"우츠보트", // 71
  L"왕눈해", // 72
  L"독파리", // 73
  L"꼬마돌", // 74
  L"데구리", // 75
  L"딱구리", // 76
  L"포니타", // 77
  L"날쌩마", // 78
  L"야돈", // 79
  L"야도란", // 80
  L"코일", // 81
  L"레어코일", // 82
  L"파오리", // 83
  L"두두", // 84
  L"두트리오", // 85
  L"쥬쥬", // 86
  L"쥬레곤", // 87
  L"질퍽이", // 88
  L"질뻐기", // 89
  L"셀러", // 90
  L"파르셀", // 91
  L"고오스", // 92
  L"고우스트", // 93
  L"팬텀", // 94
  L"롱스톤", // 95
  L"슬리프", // 96
  L"슬리퍼", // 97
  L"크랩", // 98
  L"킹크랩", // 99
  L"찌리리공", // 100
  L"붐볼", // 101
  L"아라리", // 102
  L"나시", // 103
  L"탕구리", // 104
  L"텅구리", // 105
  L"시라소몬", // 106
  L"홍수몬", // 107
  L"내루미", // 108
  L"또가스", // 109
  L"또도가스", // 110
  L"뿔카노", // 111
  L"코뿌리", // 112
  L"럭키", // 113
  L"덩쿠리", // 114
  L"캥카", // 115
  L"쏘드라", // 116
  L"시드라", // 117
  L"콘치", // 118
  L"왕콘치", // 119
  L"별가사리", // 120
  L"아쿠스타", // 121
  L"마임맨", // 122
  L"스라크", // 123
  L"루주라", // 124
  L"에레브", // 125
  L"마그마", // 126
  L"쁘사이저", // 127
  L"켄타로스", // 128
  L"잉어킹", // 129
  L"갸라도스", // 130
  L"라프라스", // 131
  L"메타몽", // 132
  L"이브이", // 133
  L"샤미드", // 134
  L"쥬피썬더", // 135
  L"부스터", // 136
  L"폴리곤", // 137
  L"암나이트", // 138
  L"암스타", // 139
  L"투구", // 140
  L"투구푸스", // 141
  L"프테라", // 142
  L"잠만보", // 143
  L"프리져", // 144
  L"썬더", // 145
  L"파이어", // 146
  L"미뇽", // 147
  L"신뇽", // 148
  L"망나뇽", // 149
  L"뮤츠", // 150
  L"뮤" // 151
};
std::wstring speciesName(int dex) {
  if(dex<1||dex>DEX_COUNT)return L"알 수 없음";
  return KOREAN_DEX_NAMES[dex];
}
std::wstring petName() {
  if(pet.isEgg()) return L"알";
  if(pet.nick[0]) return fromUtf8(pet.nick);
  return speciesName(pet.speciesId);
}
void drawPet(HDC dc) {
  COLORREF accent=pet.isEgg()?RGB(248,220,155):rgb565(DEX_TBL[pet.speciesId].accent);
  HBRUSH shadow=CreateSolidBrush(RGB(220,232,238)); HPEN rim=CreatePen(PS_SOLID,9,RGB(204,164,112));
  auto ob=SelectObject(dc,shadow);auto op=SelectObject(dc,rim);Ellipse(dc,210,92,510,392);
  SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(shadow);DeleteObject(rim);
  HBRUSH inner=CreateSolidBrush(RGB(242,247,242));HPEN no=CreatePen(PS_NULL,0,RGB(242,247,242));
  ob=SelectObject(dc,inner);op=SelectObject(dc,no);Ellipse(dc,224,106,496,378);
  SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(inner);DeleteObject(no);
  const char* const* sprite=nullptr;
  if(pet.isEgg()) sprite=SPR_EGG;
  else if(pet.speciesId>=1&&pet.speciesId<=151&&desktopSprites[pet.speciesId].actions[0].w>0) {
    int saved=SaveDC(dc); HRGN mask=CreateEllipticRgn(224,106,496,378); SelectClipRgn(dc,mask);
    int actionId=0;
    DWORD now=GetTickCount();
    if(pet.eating()) actionId=4;
    else if(pet.sleeping) actionId=3;
    else if(pet.evolving()) actionId=8;
    else if(pet.showHeart()) actionId=9;
    else if(now<transientActionUntil) actionId=transientAction;
    else if(pet.energy<20) actionId=10;
    else if(pet.joy<20) actionId=11;
    if(pet.evolving()&&pet.prevSpeciesId>=1&&pet.prevSpeciesId<=151) {
      float t=std::clamp(pet.evolveT(),0.0f,1.0f);
      BYTE oldAlpha=(BYTE)(255.0f*(1.0f-t));
      BYTE newAlpha=(BYTE)(255.0f*t);
      RECT spriteBounds{245,130,475,360};
      drawDesktopSprite(dc,pet.prevSpeciesId,spriteBounds,8,now,oldAlpha);
      drawDesktopSprite(dc,pet.speciesId,spriteBounds,8,now,newAlpha);
      if(t>0.38f&&t<0.62f) {
        int pulse=(int)(12.0f*(1.0f-std::abs(t-0.5f)*2.0f));
        HPEN glow=CreatePen(PS_SOLID,3,RGB(255,231,155));
        HGDIOBJ oldPen=SelectObject(dc,glow); HGDIOBJ oldBrush=SelectObject(dc,GetStockObject(HOLLOW_BRUSH));
        Ellipse(dc,224-pulse,106-pulse,496+pulse,378+pulse);
        SelectObject(dc,oldBrush);SelectObject(dc,oldPen);DeleteObject(glow);
      }
    } else {
      drawDesktopSprite(dc,pet.speciesId,RECT{245,130,475,360},actionId,now);
    }
    RestoreDC(dc,saved); DeleteObject(mask);
    if(pet.showHeart()) text(dc,L"♥",RECT{410,145,470,205},36,RGB(255,130,150),true);
    if(pet.eating()) text(dc,L"냠!",RECT{250,145,310,190},22,RGB(255,226,150),true);
    return;
  } else {
    int id=pet.speciesId;
    if(id>=1&&id<=3)sprite=SPECIES[SP_BULBASAUR+(id-1)].sprite;
    else if(id>=4&&id<=6)sprite=SPECIES[SP_CHARMANDER+(id-4)].sprite;
    else if(id>=7&&id<=9)sprite=SPECIES[SP_SQUIRTLE+(id-7)].sprite;
  }
  if(sprite) {
    const int scale=5, sx=280, sy=155;
    for(int row=0;row<32;row++) for(int col=0;col<32;col++) {
      uint16_t color=spriteColor(sprite[row][col]);
      if(color) {
        HBRUSH px=CreateSolidBrush(rgb565(color));
        RECT pr{sx+col*scale,sy+row*scale,sx+(col+1)*scale,sy+(row+1)*scale};
        FillRect(dc,&pr,px);DeleteObject(px);
      }
    }
  } else {
    HBRUSH body=CreateSolidBrush(accent);HPEN outline=CreatePen(PS_SOLID,3,RGB(75,62,53));
    ob=SelectObject(dc,body);op=SelectObject(dc,outline);Ellipse(dc,286,175,434,320);
    SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(body);DeleteObject(outline);
    HBRUSH eye=CreateSolidBrush(RGB(35,38,46));ob=SelectObject(dc,eye);
    Ellipse(dc,326,220,340,239);Ellipse(dc,382,220,396,239);
    SelectObject(dc,ob);DeleteObject(eye);
  }
  if(pet.showHeart()) text(dc,L"♥",RECT{410,145,470,205},36,RGB(255,130,150),true);
  if(pet.eating()) text(dc,L"냠!",RECT{250,145,310,190},22,RGB(255,226,150),true);
}
void drawHome(HDC dc) {
  drawPet(dc);
  text(dc,petName(),RECT{180,395,540,430},24,RGB(75,61,49),true);
  std::wstring meta=pet.isEgg()?L"새로운 동료를 기다리는 중":L"레벨 "+std::to_wstring(pet.level())+L"  ·  함께한 시간 "+std::to_wstring(pet.ageMinutes/60)+L"시간";
  text(dc,meta,RECT{120,428,600,458},15,RGB(125,105,86));
  bar(dc,142,470,L"배고픔",pet.fullness,RGB(237,166,75));
  bar(dc,142,503,L"기분",pet.joy,RGB(236,130,158));
  bar(dc,142,536,L"에너지",pet.energy,RGB(105,168,220));
  bar(dc,142,569,L"청결",pet.hygiene,RGB(100,185,145));
  if(pet.awaitingStarter()) {
    text(dc,L"첫 파트너를 선택하세요",RECT{90,615,630,648},18,RGB(95,73,52),true);
    button(dc,88,658,170,54,L"이상해씨");
    button(dc,275,658,170,54,L"파이리");
    button(dc,462,658,170,54,L"꼬부기");
  } else if(pet.isEgg()) {
    text(dc,L"알을 클릭해 부화시켜 보세요 (3회)",RECT{90,615,630,648},17,RGB(95,73,52),true);
    button(dc,250,658,220,54,L"알 톡톡!");
  } else {
    button(dc,44,625,145,54,L"먹이 주기");
    button(dc,202,625,145,54,L"놀아주기");
    button(dc,360,625,145,54,L"목욕시키기");
    button(dc,518,625,158,54,pet.sleeping?L"깨우기":L"재우기");
    if(pet.wantEvolveButton()) button(dc,250,690,220,44,L"진화하기",true);
    else if(pet.wantFarewellButton()) button(dc,250,690,220,44,L"작별 인사",true);
    else if(pet.canRunawayNow()) text(dc,L"돌봄이 필요해요!",RECT{200,690,520,730},16,RGB(180,75,65),true);
  }
}
void drawStats(HDC dc) {
  text(dc,L"상태 정보",RECT{100,105,620,150},27,RGB(76,60,47),true);
  text(dc,petName()+L"  ·  Lv."+std::to_wstring(pet.level()),RECT{100,160,620,198},21,RGB(88,71,56),true);
  const std::wstring rows[]={L"공격력  "+std::to_wstring(pet.atkStat()),L"방어력  "+std::to_wstring(pet.defStat()),L"속도  "+std::to_wstring(pet.speStat()),L"몸무게  "+std::to_wstring(pet.weight),L"유대감  "+std::to_wstring(pet.bond),L"돌봄 실수  "+std::to_wstring(pet.careMistakes)};
  for(int i=0;i<6;i++) text(dc,rows[i],RECT{150,230+i*55,570,270+i*55},20,RGB(83,68,55));
  text(dc,L"훈련은 아래 버튼에서 진행할 수 있습니다.",RECT{100,590,620,630},15,RGB(120,100,80));
  button(dc,240,650,240,58,L"힘 훈련하기");
}
int gameSecondsLeft(){DWORD elapsed=GetTickCount()-gameStarted;return elapsed>=15000?0:(int)((15000-elapsed+999)/1000);}
std::wstring miniBubbleText() {
  if(pet.isEgg()) return L"톡톡! 부화시켜 주세요.";
  if(pet.sleeping) return L"Zzz... 잘 자요!";
  if(pet.fullness<25) return L"배고파요... 먹이 주세요!";
  if(pet.hygiene<25) return L"씻고 싶어요!";
  if(pet.energy<25) return L"졸려요...";
  if(pet.joy<25) return L"같이 놀아요!";
  if(pet.showHeart()) return L"좋아요! 행복해요 ♥";
  return L"오늘도 함께 놀아요!";
}
void drawMini(HWND hwnd) {
  PAINTSTRUCT ps{}; HDC dc=BeginPaint(hwnd,&ps);
  RECT rc{}; GetClientRect(hwnd,&rc);
  HBRUSH transparent=CreateSolidBrush(kMiniTransparent); FillRect(dc,&rc,transparent); DeleteObject(transparent);
  fillRound(dc,RECT{7,7,173,53},RGB(255,250,240),RGB(205,170,125),14);
  text(dc,miniBubbleText(),RECT{12,10,168,50},13,RGB(76,60,47),true);
  if(!pet.isEgg()&&pet.speciesId>=1&&pet.speciesId<=151) {
    int action=pet.sleeping?3:(pet.eating()?4:(pet.showHeart()?9:(pet.energy<20?10:0)));
    drawDesktopSprite(dc,pet.speciesId,RECT{35,58,145,170},action,GetTickCount());
  } else {
    text(dc,L"알",RECT{35,65,145,165},54,RGB(120,90,55),true);
  }
  EndPaint(hwnd,&ps);
}
void placeMini(HWND hwnd) {
  RECT work{}; SystemParametersInfoW(SPI_GETWORKAREA,0,&work,0);
  const int width=180,height=185;
  SetWindowPos(hwnd,HWND_TOPMOST,work.right-width-18,work.bottom-height-8,width,height,SWP_NOACTIVATE|SWP_SHOWWINDOW);
}
void showMini() {
  if(!miniHwnd) return;
  miniMode=true; placeMini(miniHwnd); InvalidateRect(miniHwnd,nullptr,FALSE);
}
void restoreMain(HWND owner) {
  miniMode=false; if(miniHwnd) ShowWindow(miniHwnd,SW_HIDE);
  ShowWindow(owner,SW_RESTORE); SetForegroundWindow(owner);
}
LRESULT CALLBACK miniProc(HWND hwnd,UINT msg,WPARAM wp,LPARAM lp) {
  switch(msg) {
    case WM_PAINT: drawMini(hwnd); return 0;
    case WM_TIMER: InvalidateRect(hwnd,nullptr,FALSE); return 0;
    case WM_LBUTTONDBLCLK: case WM_NCLBUTTONDBLCLK: restoreMain(mainHwnd); return 0;
    case WM_NCHITTEST: return HTCAPTION;
    case WM_RBUTTONUP: case WM_NCRBUTTONUP: {
      const bool nonClient=(msg==WM_NCRBUTTONUP);
      HMENU menu=CreatePopupMenu();
      AppendMenuW(menu,MF_STRING,1,L"TamaPoke 열기");
      AppendMenuW(menu,MF_STRING,2,L"미니 모드 종료");
      POINT pt{GET_X_LPARAM(lp),GET_Y_LPARAM(lp)}; if(!nonClient) ClientToScreen(hwnd,&pt);
      SetForegroundWindow(hwnd); int cmd=TrackPopupMenu(menu,TPM_RETURNCMD|TPM_RIGHTBUTTON,pt.x,pt.y,0,hwnd,nullptr);
      DestroyMenu(menu);
      if(cmd==1) restoreMain(mainHwnd);
      else if(cmd==2) { miniMode=false; ShowWindow(hwnd,SW_HIDE); }
      return 0;
    }
    case WM_ERASEBKGND: return 1;
  }
  return DefWindowProcW(hwnd,msg,wp,lp);
}
void drawMore(HDC dc) {
  text(dc,L"더보기",RECT{100,105,620,150},27,RGB(76,60,47),true);
  text(dc,L"미니게임 · 설정 · 포켓몬 이름",RECT{100,155,620,190},16,RGB(120,100,80));
  if(gameActive) {
    text(dc,L"타이밍 캐치  ·  남은 시간 "+std::to_wstring(gameSecondsLeft())+L"초",RECT{90,215,630,255},20,RGB(76,60,47),true);
    text(dc,L"점수 "+std::to_wstring(gameScore),RECT{90,265,630,300},18,RGB(88,71,56),true);
    HBRUSH b=CreateSolidBrush(RGB(237,166,75)); HPEN p=CreatePen(PS_SOLID,3,RGB(160,110,55));
    auto ob=SelectObject(dc,b);auto op=SelectObject(dc,p);Ellipse(dc,targetX-30,targetY-30,targetX+30,targetY+30);
    SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(b);DeleteObject(p);
    text(dc,L"원을 빠르게 클릭하세요!",RECT{120,500,600,540},18,RGB(95,73,52),true);
  } else {
    button(dc,180,235,360,58,L"미니게임 시작",true);
    button(dc,180,315,360,58,L"포켓몬 이름 변경");
    button(dc,180,395,360,58,audioEnabled()?L"효과음: 켜짐":L"효과음: 꺼짐");
    text(dc,L"15초 동안 움직이는 표적을 클릭해 점수를 얻으세요.",RECT{90,485,630,530},15,RGB(120,100,80));
    text(dc,L"효과음 설정은 다음 실행에도 유지됩니다.",RECT{90,535,630,575},14,RGB(120,100,80));
    button(dc,180,605,360,54,L"미니 펫 모드 시작",true);
  }
}
void drawDex(HDC dc) {
  text(dc,L"포켓몬 도감",RECT{100,100,620,150},27,RGB(76,60,47),true);
  text(dc,L"등록 "+std::to_wstring(pet.registeredCount())+L" / 151",RECT{100,150,620,190},20,RGB(88,71,56),true);
  int start=dexPage*24+1;
  for(int i=0;i<24;i++){
    int id=start+i;if(id>151)break;
    int col=i%4,row=i/4,x=48+col*158,y=215+row*62;
    bool seen=pet.isRegistered(id);
    fillRound(dc,RECT{x,y,x+145,y+50},seen?RGB(255,249,236):RGB(235,231,222),RGB(216,204,184),10);
    std::wstring name=seen?speciesName(id):L"???";
    if(seen) drawDesktopSprite(dc,id,RECT{x+5,y+5,x+48,y+47},0,GetTickCount());
    else {
      HBRUSH silhouette=CreateSolidBrush(RGB(210,205,195));
      HBRUSH oldBrush=(HBRUSH)SelectObject(dc,silhouette);
      Ellipse(dc,x+15,y+12,x+38,y+39);
      SelectObject(dc,oldBrush);DeleteObject(silhouette);
    }
    text(dc,std::to_wstring(id)+L". "+name,RECT{x+50,y+3,x+141,y+47},12,seen?RGB(75,62,51):RGB(155,148,137),seen);
  }
  button(dc,240,650,105,48,L"이전");
  text(dc,std::to_wstring(dexPage+1)+L" / 7",RECT{350,650,430,698},16,RGB(90,75,60),true);
  button(dc,440,650,105,48,L"다음");
}
void paint(HWND hwnd) {
  PAINTSTRUCT ps{};HDC dc=BeginPaint(hwnd,&ps);
  RECT client{};GetClientRect(hwnd,&client);
  HBRUSH bg=CreateSolidBrush(RGB(248,242,231));FillRect(dc,&client,bg);DeleteObject(bg);
  text(dc,L"TamaPoke",RECT{20,15,700,62},32,RGB(79,62,49),true);
  text(dc,L"나만의 포켓몬을 돌보는 작은 데스크톱 게임",RECT{20,60,700,88},14,RGB(133,112,91));
  if(page==0)drawHome(dc);else if(page==1)drawStats(dc);else if(page==2)drawDex(dc);else drawMore(dc);
  fillRound(dc,RECT{38,730,682,770},RGB(238,228,211),RGB(224,210,188),12);
  text(dc,notice,RECT{50,733,670,767},14,RGB(105,84,65));
  button(dc,28,790,145,48,L"돌보기",page==0);
  button(dc,190,790,145,48,L"상태",page==1);
  button(dc,352,790,145,48,L"도감",page==2);
  button(dc,514,790,178,48,L"더보기",page==3);
  EndPaint(hwnd,&ps);
}
LRESULT CALLBACK renameProc(HWND h,UINT m,WPARAM w,LPARAM l) {
  static HWND edit=nullptr;
  switch(m) {
    case WM_CREATE: {
      edit=CreateWindowExW(WS_EX_CLIENTEDGE,L"EDIT",petName().c_str(),WS_CHILD|WS_VISIBLE|ES_AUTOHSCROLL,22,22,310,32,h,(HMENU)101,GetModuleHandleW(nullptr),nullptr);
      CreateWindowW(L"BUTTON",L"저장",WS_CHILD|WS_VISIBLE|BS_DEFPUSHBUTTON,160,72,80,34,h,(HMENU)IDOK,GetModuleHandleW(nullptr),nullptr);
      CreateWindowW(L"BUTTON",L"취소",WS_CHILD|WS_VISIBLE,250,72,80,34,h,(HMENU)IDCANCEL,GetModuleHandleW(nullptr),nullptr);
      SetFocus(edit); SendMessageW(edit,EM_SETSEL,0,-1); return 0;
    }
    case WM_COMMAND:
      if(LOWORD(w)==IDOK) {
        wchar_t wide[64]{}; GetWindowTextW(edit,wide,64);
        char utf[64]{}; int n=WideCharToMultiByte(CP_UTF8,0,wide,-1,utf,sizeof(utf),nullptr,nullptr);
        if(n>0&&n<=12&&wide[0]) {pet.rename(utf);DestroyWindow(h);}
        else MessageBoxW(h,L"이름은 UTF-8 기준 11바이트 이내로 입력해 주세요.",L"TamaPoke",MB_OK|MB_ICONINFORMATION);
        return 0;
      }
      if(LOWORD(w)==IDCANCEL){DestroyWindow(h);return 0;} break;
    case WM_CLOSE: DestroyWindow(h);return 0;
  }
  return DefWindowProcW(h,m,w,l);
}
void renamePet(HWND owner) {
  static bool registered=false;
  if(!registered){WNDCLASSW wc{};wc.lpfnWndProc=renameProc;wc.hInstance=GetModuleHandleW(nullptr);wc.lpszClassName=L"TamaPokeRenameDialog";wc.hCursor=LoadCursor(nullptr,IDC_IBEAM);wc.hbrBackground=(HBRUSH)(COLOR_WINDOW+1);RegisterClassW(&wc);registered=true;}
  HWND dlg=CreateWindowExW(WS_EX_DLGMODALFRAME,L"TamaPokeRenameDialog",L"포켓몬 이름 변경",WS_CAPTION|WS_SYSMENU|WS_POPUP|WS_VISIBLE,0,0,360,150,owner,nullptr,GetModuleHandleW(nullptr),nullptr);
  RECT wr{};GetWindowRect(owner,&wr);SetWindowPos(dlg,nullptr,wr.left+(wr.right-wr.left-360)/2,wr.top+(wr.bottom-wr.top-150)/2,0,0,SWP_NOSIZE|SWP_NOZORDER);
  EnableWindow(owner,FALSE); MSG msg{};while(IsWindow(dlg)&&GetMessageW(&msg,nullptr,0,0)>0){TranslateMessage(&msg);DispatchMessageW(&msg);}EnableWindow(owner,TRUE);SetActiveWindow(owner);
}
void act(HWND hwnd,int x,int y) {
  if(y>=790&&y<=838){page=x<180?0:x<342?1:x<505?2:3;InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page==3) {
    if(gameActive) {
      if(x>=targetX-42&&x<=targetX+42&&y>=targetY-42&&y<=targetY+42) {
        gameScore++; targetX=110+std::rand()%500; targetY=320+std::rand()%150;
      }
      InvalidateRect(hwnd,nullptr,FALSE);return;
    }
    if(y>=225&&y<=300) {gameActive=true;gameScore=0;gameStarted=GetTickCount();targetX=360;targetY=350;notice=L"표적을 클릭해 점수를 올려보세요!";InvalidateRect(hwnd,nullptr,FALSE);return;}
    if(y>=305&&y<=380) {renamePet(hwnd);InvalidateRect(hwnd,nullptr,FALSE);return;}
    if(y>=385&&y<=465) {audioSetEnabled(!audioEnabled());notice=audioEnabled()?L"효과음을 켰어요.":L"효과음을 껐어요.";InvalidateRect(hwnd,nullptr,FALSE);return;}
    if(y>=590&&y<=675) {showMini();notice=L"미니 펫이 화면 위에 나타났어요. 더블클릭하면 게임으로 돌아옵니다.";InvalidateRect(hwnd,nullptr,FALSE);return;}
    return;
  }
  if(page==2&&y>=640&&y<=710){if(x<360&&dexPage>0)dexPage--;else if(x>430&&dexPage<6)dexPage++;InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page==1&&y>=640&&y<=715){uint8_t gain=pet.trainStrength(24);notice=L"힘 훈련 완료! 상승량 "+std::to_wstring(gain);InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page!=0)return;
  if(pet.awaitingStarter()&&y>=650&&y<=720){
    int id=x<265?1:x<455?4:7;pet.chooseStarter(id);notice=L"첫 파트너를 선택했어요! 이제 알을 세 번 톡톡 두드려 부화시켜 보세요." ;InvalidateRect(hwnd,nullptr,FALSE);return;
  }
  if(pet.isEgg()&&y>=100&&y<=400){pet.eggTap();notice=L"알을 톡톡 두드렸어요!";InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(y>=620&&y<=685){
    if(x<195){pet.feedBerry(0);notice=L"맛있는 베리를 줬어요!";}
    else if(x<352){pet.playResult(5);pet.caress();transientAction=6;transientActionUntil=GetTickCount()+1100;notice=L"함께 놀았어요!";}
    else if(x<510){pet.clean();notice=L"깨끗하게 목욕했어요!";}
    else {pet.toggleLight();notice=pet.sleeping?L"잘 자요!":L"일어났어요!";}
    InvalidateRect(hwnd,nullptr,FALSE);return;
  }
  if(y>=685&&y<=730&&x>=240&&x<=480){
    if(pet.wantEvolveButton()){pet.evolve();notice=L"진화가 시작됐어요!";}
    else if(pet.wantFarewellButton()){if(MessageBoxW(hwnd,L"정말 작별 인사를 할까요?\n작별 후에는 새로운 알이 찾아옵니다.",L"TamaPoke",MB_YESNO|MB_ICONQUESTION|MB_DEFBUTTON2)==IDYES){pet.startFarewell();notice=L"작별 인사를 시작했어요.";}else{pet.declineFarewell();notice=L"조금 더 함께하기로 했어요.";}}
    InvalidateRect(hwnd,nullptr,FALSE);return;
  }
  if(y>=100&&y<=400){
    pet.caress();
    transientAction=(x<320)?1:((x>400)?2:7);
    transientActionUntil=GetTickCount()+900;
    notice=L"쓰다듬어 줬어요!";
    InvalidateRect(hwnd,nullptr,FALSE);
  }
}
LRESULT CALLBACK proc(HWND hwnd,UINT msg,WPARAM wp,LPARAM lp){
  switch(msg){
    case WM_CREATE:{std::srand((unsigned)std::time(nullptr));audioBegin();loadDesktopSprites();pet.begin();time_t now=std::time(nullptr);pet.syncClock((uint32_t)now);lastClockSync=now;SetTimer(hwnd,1,250,nullptr);return 0;}
    case WM_SIZE: if(wp==SIZE_MINIMIZED) showMini(); else if(wp==SIZE_RESTORED&&miniHwnd) {miniMode=false;ShowWindow(miniHwnd,SW_HIDE);} return 0;
    case WM_TIMER:{pet.update(millis());if(GetTickCount()>=transientActionUntil)transientAction=0;if(gameActive&&GetTickCount()-gameStarted>=15000){gameActive=false;pet.playResult((uint8_t)(gameScore>255?255:gameScore));notice=L"미니게임 종료! 획득 점수: "+std::to_wstring(gameScore);page=3;}time_t now=std::time(nullptr);if(now-lastClockSync>=60){pet.setClock((uint32_t)now);lastClockSync=now;}if(pet.savePending())pet.flushSave();InvalidateRect(hwnd,nullptr,FALSE);if(miniHwnd&&IsWindowVisible(miniHwnd))InvalidateRect(miniHwnd,nullptr,FALSE);return 0;}
    case WM_LBUTTONUP:act(hwnd,LOWORD(lp),HIWORD(lp));return 0;
    case WM_PAINT:paint(hwnd);return 0;
    case WM_ERASEBKGND:return 1;
    case WM_DESTROY:KillTimer(hwnd,1);pet.setClock((uint32_t)std::time(nullptr));destroyDesktopBitmaps();PostQuitMessage(0);return 0;
    default:return DefWindowProcW(hwnd,msg,wp,lp);
  }
}
}
int WINAPI wWinMain(HINSTANCE instance,HINSTANCE,PWSTR,int show){
  WNDCLASSW wc{};wc.lpfnWndProc=proc;wc.hInstance=instance;wc.lpszClassName=kClassName;
  wc.hCursor=LoadCursor(nullptr,IDC_HAND);wc.hbrBackground=(HBRUSH)(COLOR_WINDOW+1);
  if(!RegisterClassW(&wc))return 1;
  WNDCLASSW mwc{};mwc.lpfnWndProc=miniProc;mwc.hInstance=instance;mwc.lpszClassName=kMiniClassName;
  mwc.hCursor=LoadCursor(nullptr,IDC_HAND);mwc.hbrBackground=CreateSolidBrush(kMiniTransparent);mwc.style=CS_DBLCLKS;
  if(!RegisterClassW(&mwc))return 1;
  RECT r{0,0,W,H};AdjustWindowRect(&r,WS_OVERLAPPED|WS_CAPTION|WS_SYSMENU|WS_MINIMIZEBOX,FALSE);
  HWND hwnd=CreateWindowExW(0,kClassName,L"TamaPoke",WS_OVERLAPPED|WS_CAPTION|WS_SYSMENU|WS_MINIMIZEBOX,
    CW_USEDEFAULT,CW_USEDEFAULT,r.right-r.left,r.bottom-r.top,nullptr,nullptr,instance,nullptr);
  if(!hwnd)return 1;
  mainHwnd=hwnd;
  miniHwnd=CreateWindowExW(WS_EX_LAYERED|WS_EX_TOOLWINDOW|WS_EX_TOPMOST|WS_EX_NOACTIVATE,kMiniClassName,L"TamaPoke Mini",
    WS_POPUP,0,0,180,185,nullptr,nullptr,instance,nullptr);
  if(!miniHwnd)return 1;
  SetLayeredWindowAttributes(miniHwnd,kMiniTransparent,0,LWA_COLORKEY);
  SetTimer(miniHwnd,2,250,nullptr);ShowWindow(miniHwnd,SW_HIDE);
  ShowWindow(hwnd,show);UpdateWindow(hwnd);
  MSG msg{};while(GetMessageW(&msg,nullptr,0,0)>0){TranslateMessage(&msg);DispatchMessageW(&msg);}
  return (int)msg.wParam;
}
