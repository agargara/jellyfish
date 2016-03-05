// Run this to start (DO NOT HIT COMMAND+PERIOD AFTERWARD)
(
var samplePath = PathName.new("/Users/agargara/programming/rhizome/jellyfish/jellyfish/pages/sounds/");
s = Server.local;
s.quit;
s.options.numOutputBusChannels = 32;
s.options.device = "Soundflower (64ch)";
s.boot;
s.waitForBoot({
~buffers = Dictionary.new;
// load all samples in directory
samplePath.files.do({|item, index|
  ~buffers.put(item.fileName.asSymbol, Buffer.read(s, item.fullPath));
});
~currentMode = "rising-fm";
 ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]).midicps;
~userList = Set.new();
// Function to distribute freqs in an array round-robin to each user
~redistributeFrequencies = {
  ~userList.do{
    |user, index|
    n.sendMsg("/"++user++"/freq/", ~freqs[index%~freqs.size]);
  }
};
~changeAndRedistribute={
  n.sendMsg("/freqs", * ~freqs);
  ~redistributeFrequencies.value();
  ~playFreqs.value();
};
~playFreqs = {
  var channels = [0,1,2,3,4,5,6].scramble;
  ~freqs.do{
    |freq, i|
    Synth(\agrlongtone, [\freq, freq, \gain, 0.1, \time, 15, \out, channels[i%7]])
  }
};
// n = NetAddr.new("pauline.mills.edu", 9000); // send messages to this address
n = NetAddr.new("localhost", 9000); // send messages to this address
thisProcess.openUDPPort(9002);
n.sendMsg("/sys/subscribe", 9002, "/"); // subscribe to all rhizome messages
thisProcess.removeOSCRecvFunc(f);
f = { |msg, time, addr|
  if(msg[0] != '/status.reply') {
    switch(msg[0],
      '/addPlayer',
      {
        if(msg[1].notNil,{
          ~userList.add(msg[1].asString);
          ("Users: "+~userList).postln;
          ~redistributeFrequencies.value();
        });
      },
      '/getPlayers',
      {
       n.sendMsg("/mode", ~currentMode);
       ("changing mode to "+~currentMode).postln;
      }
    );
  }
};
thisProcess.addOSCRecvFunc(f);
});
)

// Utility messages
n.sendMsg("/global/gain/", 0);
n.sendMsg("/global/gain/", 1);


//
//
// THE NIGHT OF LANGIS VVOLF IS UPON US
//
//

// PART 1: BUBBLES
(
~currentMode = "bubbles";
n.sendMsg("/mode", ~currentMode);
)

// PART 2: LONG TONES
//
(
Tdef(\changeFreqs, {
  var interval = 9;
  ~currentMode = "long-tones";
  n.sendMsg("/mode", ~currentMode);
  1.do{
    1.do{
      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+4).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
      ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+8).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
    };
    1.do{
      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-1).midicps;
     ~changeAndRedistribute.value();
      interval.wait;
      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+3).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
      ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+7).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
    };
    1.do{
      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-2).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+2).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
      ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+6).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
    };
    1.do{
      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]-3).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]+1).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
      ~freqs = ([48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74]+5).midicps;
      ~changeAndRedistribute.value();
      interval.wait;
    };
  }
}).play;
)

// PART 3:
Tdef(\changeFreqs).stop;
(
var interval = 10;
Tdef(\changeModes, {
  loop{
    ~currentMode = "static-pads";
    n.sendMsg("/mode", ~currentMode);
    interval.wait;
    ~currentMode = "rising-fm";
    n.sendMsg("/mode", ~currentMode);
    interval.wait;
    ~currentMode = "noise";
    n.sendMsg("/mode", ~currentMode);
    interval.wait;
    ~currentMode = "drums";
    n.sendMsg("/mode", ~currentMode);
    interval.wait;
  }
}).play;
)
Tdef(\changeModes).stop;
// THE BEATZ
(
var interval = 0.08;
var step = 0;
var kickStep = 3;
var snareStep = 5;
var loopCount = 0;
Tdef(\playDrums, {
  loop{
    var curBuf = ~buffers.values.choose;
    var outChannel = [0,1,2,3,4,5,6].choose;
    if((step == 0)||(step == kickStep),{
      if(1.0.rand>0.1,
        {curBuf = ~buffers.at("bd.wav".asSymbol);
          outChannel = 0;
      });
    });
    if((step == 5)||(step == snareStep),{
      if(1.0.rand>0.03,
        {curBuf = ~buffers.at("clap.wav".asSymbol);
          outChannel = 5;
      });
    });
    Synth(\agrsampler, [\buffer, curBuf, \numFrames, curBuf.numFrames,
      \sampleRate, curBuf.sampleRate, \rate, rrand(0.25,2), \amp, 0.7, \out, outChannel]);
    ([2].choose*interval).wait;
    step = (step+1)%8;
    loopCount = (loopCount+1)%128;
    if(loopCount%32 == 0,{
      snareStep = [2,5,7].wchoose([4,10,1].normalizeSum);
      kickStep = [2,3,4,5,7].wchoose([3,5,1,1,2].normalizeSum);
    });
  }
}).play;
)
Tdef(\playDrums).stop;


(
var interval = 0.1;
var step = 0;
Tdef(\playDrums, {
  loop{
    var curBuf = ~buffers.values.choose;
    Synth(\agrsampler, [\buffer, curBuf, \numFrames, curBuf.numFrames,
      \sampleRate, curBuf.sampleRate, \rate, rrand(0.25,16), \amp, 0.5, \out, [0,1,2,3,4,5,6].choose]);
    ([1,2,3,4,5,6].choose*interval).wait;
    step = (step+1)%8;
  }
}).play;
)




(
~freqs = ([40]).midicps;
      ~changeAndRedistribute.value();
)

(
SynthDef(\agrlongtone, {
  |out=0, freq=230, time=1, gain=0.5|
  var sound, ampenv, filterenv, tremolo, depth;

  // wahwah filter
  filterenv = EnvGen.kr(Env.linen(
    Rand(time/3.1),
    Rand(time/3.1),
    Rand(time/3.1)).range(Rand(freq*0.9,freq*1.5),Rand(freq*1.5,freq*3.0))
  );

  ampenv = EnvGen.kr(Env.linen(
    Rand(time/3.1),
    Rand(time/3.1),
    Rand(time/3.1)),doneAction:2);

  sound = Select.ar(IRand(0,3), [
    Pulse.ar(freq,0.5).tanh,
    LFTri.ar(freq,0).tanh,
    Pulse.ar(freq,0.5).tanh,
    LFSaw.ar(freq,0.5).tanh
  ]);

  sound = sound*0.15; //  attenuate
  sound = RLPF.ar(sound, filterenv, LFNoise2.kr(Rand(0.001,0.1), 0.3, 0.5) );
  depth = Rand(0.1,0.2);
  tremolo = SinOsc.ar(Select.kr(IRand(0,6),[3,4,5,6,7,1,2]), 0, depth, 1-depth);
  sound = sound + (DelayC.ar(sound, 1/4,
    {Select.kr(IRand(0,3),[1/4,1/8,1/6,1/12])}.dup(2))
  * Rand(0.0,1.0)); // random delay
  5.do{sound = HPF.ar(sound, 100)}; // high pass unneeded lower freqs
  sound = sound * ampenv * tremolo * gain; // volume control
  Out.ar(out,Mix(sound.tanh)); // mono
}).store;

SynthDef(\agrsampler, {
  arg out=0, rate=1.0, buffer, numFrames=0, startPos = 0, endPos = 1, t_reset = 0, interpolation=1, amp=1, sampleRate=44100;
  var sig, phase;
  var length = endPos - startPos;
  phase = Line.ar(startPos * numFrames, endPos * numFrames, (numFrames/sampleRate) * BufRateScale.kr(buffer) * rate, doneAction: 2);
  sig = BufRd.ar(2, buffer, phase, 1, loop:0, interpolation:interpolation);
  Out.ar(out, (sig * amp)); // stereo out
}).store;
)

Synth(\agrlongtone, [\freq, 65.midicps, \gain, 1, \time, 15, \out, 5])

      ~freqs = ([ 48, 50, 51, 53, 55, 56, 58, 60, 62, 63, 65, 67, 68, 70, 74 ]).midicps;
      ~changeAndRedistribute.value();

