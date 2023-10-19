$( document ).ready(function(){
  $("#debug").text("");
  $("#ocean").hide();
  $("#instructions").css("opacity", "0");
  $(function() {
    FastClick.attach(document.body);
  });
  var context = new AudioContext();
  var isMouseDown = false;
  var touchedElement = null;
  var hasStarted = false;
  var myId;
  //var modes = ['long-tones','rising-fm','noise','drums', 'bubbles','broken-drums', 'loopy', 'one-note', 'the-end'];
  var modes = ['long-tones','rising-fm','noise','drums', 'bubbles','broken-drums', 'loopy'];
  var modeIndex = 4;
  var timesPressed = 0;
  previousMode = "";
  //switchModes(modes[modeIndex]);
  var freqs = [130.8127826503, 146.8323839587, 155.56349186104, 174.6141157165, 195.99771799087, 207.65234878997, 233.08188075904, 261.6255653006, 293.66476791741, 311.12698372208, 349.228231433, 391.99543598175, 415.30469757995, 466.16376151809, 587.32953583482];
  var loopyFreqs;
  var loopyDurs;
  var loopyTotalDur;
  resetLoopyFreqs();
  // Global vars for longTones / oneNote
  var globalGain = context.createGain();
  globalGain.connect(context.destination);
  var biquadFilter = context.createBiquadFilter();
  biquadFilter.type = "lowpass";
  biquadFilter.frequency.value = 1000;
  biquadFilter.Q.value = 10;
  // connect filter to output
  biquadFilter.connect(globalGain);
  var myFreq = 0;
  var currentIndex = 0;
  var freqIndexArray = [];
  for(i=0; i<freqs.length; i++){
    freqIndexArray[i] = i;
  }
  freqIndexArray = shuffleArray(freqIndexArray);
  var lastIndex = freqIndexArray[freqIndexArray.length-1];
  var currentStaticEnv = null;
  var currentStaticVCO = null;

  // Global vars for pads, etc.
  var pads = new Array();
  var whiteNoise=null;
  var pinkNoise = new PinkNoise(context, 0.4);
  var noiseSine;
  var bubbleSine;
  var bubbleFreq;
  var bubbleRate = 1.01;
  var bubbleFreqOffset = 0;
  var loopySaw;
  var fadingLoopy = false;
  var fadingInOneNote = true;
  var oneNoteOsc = null;
  var oneNoteGainChange = 0.00001;
  var distortion = new Distortion(context);
  // Start always-on nodes...
  var loopyVCA = new VCA(context);
  var loopyEnvelope = new AR_EnvelopeGenerator(context,0.5);
  loopyEnvelope.attackTime=0.01;
  loopyEnvelope.releaseTime=0.1;
  loopyEnvelope.connect(loopyVCA.amplitude);
  // connect vca to filter
  loopyVCA.connect(biquadFilter);
  var padEnv;

  // misc
  var shades = ["#26256F", "#302F8C", "#3937A6", "#4140BF", "#4A48D9", "#5351F2", "#5755FF"];
  var freqIndex = 0;
  var lastClick=Date.now();
  var interval = 1000000;
  var isPaused = false;
  var bubbling=false;
  var bubbleFade = null;
  var bubbleEvent = null;
  var bubbleAnimation = null;
  var bubbleAnimationRate = 200;
  var bubblesPlaying = false;
  var loopyTimeout = null;
  var loopyAnimation = null;
  var oneNoteTimeout = null;
  // Get sound files
  var soundBuffers = new Array(); // Buffer for files
  var myXMLrequests = new Array();
  var paths = [
    "sounds/bd.wav", "sounds/clap.wav", "sounds/ch.wav", 
    "sounds/idm1.wav", "sounds/idm2.wav", "sounds/idm3.wav", 
    "sounds/idm4.wav", "sounds/idm5.wav", "sounds/idm6.wav", 
    "sounds/idm7.wav", "sounds/rim.wav"];
  for(var i=0; i<paths.length; i++){
    (function (i){
      myXMLrequests[i] = new XMLHttpRequest();
      var path = paths[i];
      myXMLrequests[i].open("GET", path, true);
      myXMLrequests[i].responseType = "arraybuffer";
      myXMLrequests[i].onload = function() {
        context.decodeAudioData(myXMLrequests[i].response, function(b){
          soundBuffers[i]=b;
        },function(err) { console.log("err(decodeAudioData): "+err);});
      }
      myXMLrequests[i].send();
    })(i);
  }

  // Initialize jRumble on jellyfish
  $('#jellyfishicon').jrumble({
    x: 10,
    y: 10,
    rotation: 4,
    speed: 0
  });
  var rumbling = false;

  // EVENTS
  $('#modeChangeButton').on('touchend mouseup touchcancel touchleave',function(e){
    e.preventDefault();
    modeIndex = (modeIndex+1) % (modes.length);
    switchModes(modes[modeIndex]);
  })

  $("#jellyarea").on('touchstart mousedown',function(e) {
    e.preventDefault();
    var theEvent = (e.originalEvent)
    if(theEvent.touches != undefined){
      theEvent = theEvent.touches[0];
    }
    isMouseDown = true;
    if(hasStarted){
      switch (modes[modeIndex]) {
        case 'long-tones':
          if(freqs.length>=1){
            longTones();
          }
          break;
        case 'rising-fm':
          risingFM(theEvent);
          break;
        case 'noise':
          noiseOn();
          break;
        case 'drums':
          drums();
          break;
        case 'bubbles':
          bubblesOn();
          break;
        case 'broken-drums':
          brokenDrums();
          break;
        case 'loopy':
          //
          break;
        case 'one-note':
          //
          break;
        case 'the-end':
          // @TODO Easter Egg?
          break;
      }
    }
  });

  $("#jellyarea").on('mousemove touchmove',function(e){
    e.preventDefault();
    if(isMouseDown){
      e = (e.originalEvent)
      if(e.touches != undefined){
        e = (e.touches[0] || e.changedTouches[0]);
      }
      // Y: 0 to screenHeight -> 8200 to 200
      biquadFilter.frequency.value = 200+((1-(e.pageY/$(window).height()))*8000);
      // X: 0 to screenWidth -> 0.1 to 20.1
      biquadFilter.Q.value = 0.1+((e.pageX/$(window).width())*20);
      
      // Y: 0 to screenHeight -> rate 1.01 to 1.07
      bubbleRate = 1.01+((1-(e.pageY/$(window).height()))*0.06);
      // X: 0 to screenWidth -> freq offset -1000 to 100
      bubbleFreqOffset = (((e.pageX/$(window).width()))*1100)-1000;
      // X+Y: 0 to screenHeight+screenWidth -> bubbleAnimationRate
      bubbleAnimationRate = 100+((
        (1-(e.pageX/$(window).width())+
        (e.pageY/$(window).height()))
      )*50);

      // X: 0 to screenWidth -> loopy env release
      if(loopyEnvelope != null){
        loopyEnvelope.releaseTime = (((e.pageX/$(window).width()))*0.3)+0.01;
      }
    }
  });

  $("#jellyarea").on('touchend mouseup touchcancel touchleave',function(e) {
    isMouseDown = false;
    e.preventDefault();
    if(!hasStarted){
      // Make a silent note to start audio on iOS
      var oscFix = context.createOscillator();
      oscFix.type = 'sine';
      oscFix.frequency.value = 230;
      oscFix.start();
      oscFix.stop();
      hasStarted = true;
      // reswitch modes
      switchModes(modes[modeIndex]);
    }else{
      if(modes[modeIndex]!='one-note'){
        $('#jellyfishicon').attr('src', "img/jellyfish-icon-straight.png");
      }
      padOff();
      noiseOff();
      bubblesOff();
    }
  });

  // make the default pads in the background
  var padFreqs;
  var padVCA = new VCA(context);
  padEnv = new AR_EnvelopeGenerator(context,0.07);
  padEnv.attackTime=1;
  padEnv.releaseTime=4;
  padEnv.connect(padVCA.amplitude);
  padVCA.connect(globalGain);

  function switchModes (mode){
    console.log(`Switch modes to: ${mode}`)
    if(mode != previousMode){
    if(mode!='one-note'){
      loopOff();
    }
    oneNoteOff();
    switch (mode) {
      case 'long-tones':
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").text("press and hold, move finger... repeat")
        }).animate({
          opacity: "1",
          color: "black"
        },2000);
        $('body').animate({
          backgroundColor: "#46459F"
        }, 100);
        break;
      case 'rising-fm':
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").html("tap upper half to rise<br>tap bottom half to descend")
        }).animate({
          opacity: "1",
          color: "white"
        },2000);
        break;
      case 'noise':
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").html("tap rhythmically<br>move finger while pressing")
        }).animate({
          opacity: "1",
          color: "black"
        },2000);
        $('body').animate({
          backgroundColor: "#6F2525"
        }, 100);
        break;
      case 'drums':
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").text("tap rhythmically")
        }).animate({
          opacity: "1",
          color: "black"
        },2000);
        $('body').animate({
          backgroundColor: "#65C44F"
        }, 100);
        break;
      case 'bubbles':
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").text("press, hold, move finger")
        }).animate({
          opacity: "1",
          color: "black"
        },2000);
        $('body').animate({
          backgroundColor: "#BCABE3"
        }, 100);
        break;
      case 'broken-drums':
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").text("tap whenever you like")
        }).animate({
          opacity: "1",
          color: "black"
        },2000);
        $('body').animate({
          backgroundColor: "#6DCB62"
        }, 100);
        break;
      case 'loopy':
        // start loop
        loopy();
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").text("press and move finger to change sound")
        }).animate({
          opacity: "1",
          color: "black"
        },2000);
        $('body').animate({
          backgroundColor: "#4998DC"
        }, 100);
        break;
      case 'one-note':
        // fade out loopy
        fadingLoopy = true;
        fadingInOneNote = true;
        // start note
        oneNote();
        $("#ocean").hide();
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").text("press and move finger to change sound");
        }).animate({
          opacity: "1",
          color: "white"
        },2000);
        $('body').animate({
          backgroundColor: "#000"
        }, 5000);
        break;
      case 'the-end':
        $("#ocean").hide();
        // change instructions
        $("#instructions").animate({
          opacity: "0"
        },2000,function(){
          $("#instructions").html("終<br>end");
        }).animate({
          opacity: "1",
          color: "white",
          top: "25%",
          "font-size": "4em",
        },2000);
        $('#jellyfishicon').attr('src', "img/jellyfish-icon-white.png");
        $("#jellyfishicon").animate({
          opacity: "0.15"
        });
        $('body').animate({
          backgroundColor: "#111"
        }, 1000);
        break;
    }
    }
    previousMode = mode;
  }

  // Maps linear range to another linear range, with clipping
  function linlin (number, min, max, newMin, newMax){
    if(number < min){
      return newMin;
    }else if(number > max){
      return newMax;
    }
    return (((number - min) * (newMax - newMin)) / (max - min)) + newMin;
  }

  // change brightness of a hexadecimal color
  function shadeColor(color, percent) {   
    var f=parseInt(color.slice(1),16),t=percent<0?0:255,p=percent<0?percent*-1:percent,R=f>>16,G=f>>8&0x00FF,B=f&0x0000FF;
    return "#"+(0x1000000+(Math.round((t-R)*p)+R)*0x10000+(Math.round((t-G)*p)+G)*0x100+(Math.round((t-B)*p)+B)).toString(16).slice(1);
  }

  // blend two colors
  function blendColors(c0, c1, p) {
    var f=parseInt(c0.slice(1),16),t=parseInt(c1.slice(1),16),R1=f>>16,G1=f>>8&0x00FF,B1=f&0x0000FF,R2=t>>16,G2=t>>8&0x00FF,B2=t&0x0000FF;
    return "#"+(0x1000000+(Math.round((R2-R1)*p)+R1)*0x10000+(Math.round((G2-G1)*p)+G1)*0x100+(Math.round((B2-B1)*p)+B1)).toString(16).slice(1);
  }

  // Modes...
  function longTones(){
    // change img
    $('#jellyfishicon').attr('src', "img/jellyfish-icon-swimming.png");
    // stop if already playing
    if(currentStaticEnv != null){
      currentStaticEnv.release();
    }
    if(currentStaticVCO != null){
      currentStaticVCO.oscillator.stop(
        context.currentTime+currentStaticEnv.releaseTime);
    }
    // make new synth, envelope on random freq from freqs
    var theFreq = freqs[freqIndexArray[currentIndex]];
    currentIndex++;
    // If the urn is empty, reshuffle
    if(currentIndex >= freqIndexArray.length){
      currentIndex = 0;
      // Reshuffle until there are no repeats unless array size is 1
      if(freqIndexArray.length > 1){
        do{
          freqIndexArray = shuffleArray(freqIndexArray);
        }while(freqIndexArray[0] == lastIndex);
        lastIndex = freqIndexArray[freqIndexArray.length-1];
      }
    }
    var myVCO = new VCO(context, 'sawtooth', theFreq, 0.5);
    currentStaticVCO = myVCO;
    var myEnv = new ASR_EnvelopeGenerator(context,0.5);
    currentStaticEnv = myEnv;
    var atk = 1;
    var rls = 4;
    myEnv.attackTime=atk;
    myEnv.releaseTime=rls;
    // connect VCO to envelope
    myVCO.connect(myEnv);
    // connect envelope to filter
    myEnv.connect(biquadFilter);
    // connect filter to output
    biquadFilter.connect(globalGain);

    myVCO.oscillator.start(context.currentTime);      
    myEnv.trigger();
  }
  function padOff(){
    if(currentStaticEnv != null){
      currentStaticEnv.release();
      if(currentStaticVCO!= null){
        currentStaticVCO.oscillator.stop(
          context.currentTime+currentStaticEnv.releaseTime);   
        currentStaticVCO = null;
      }
      currentStaticEnv = null;
    }
  }
  function risingFM(theEvent){
    // change img
    $('#jellyfishicon').attr('src', "img/jellyfish-icon-swimming.png");
    // animate img
    $( "#jellyfishicon" ).animate({
      top: "-=10",
    }, 80, function(){
      $( "#jellyfishicon" ).animate({
        top: "10px",
      }, 80).attr('src', "img/jellyfish-icon-straight.png");
    });
    if (theEvent.pageY < ($(window).height()/2)){ // touch top
      freqIndex = freqIndex + 1;
    }else{ // touch bottom
      freqIndex = freqIndex - 1;
    }
    $('body').stop(true,false);
    $('body').animate({
      backgroundColor: shadeColor("#26256F",linlin(
        freqIndex, 0, freqs.length-1, -0.8, 0.6)),
    }, 100);
    if(freqIndex >= freqs.length){
      freqIndex = freqs.length-1;
    }else if(freqIndex < 0){
      freqIndex = 0;
    }
    // make a new synth
    var vco = new VCO(context, 'sine', 230, 1);
    var mod = new VCO(context, 'sine', 4, 460);
    var atk = 0.1;
    var rls = 0.5;
    vco.setFrequency(freqs[Math.floor(freqIndex)]);
    var mult = [1.0,2.0,1.5,(5/3),(5/2)][Math.floor(Math.random()*5)];
    mod.setFrequency(mult*freqs[Math.floor(freqIndex)]);
    // connect gain of modulator to freq of carrier
    mod.gain.connect(vco.oscillator.frequency);
    // connect carrier to VCA 
    var vca = new VCA(context);
    vco.connect(vca);
    var envelope = new AR_EnvelopeGenerator(context,0.5);
    envelope.attackTime=atk;
    envelope.releaseTime=rls;
    envelope.connect(vca.amplitude);
    // connect VCA to output
    vca.connect(globalGain);
    vco.oscillator.start(context.currentTime);
    vco.oscillator.stop(context.currentTime+atk+rls);
    mod.oscillator.start(context.currentTime);
    mod.oscillator.stop(context.currentTime+atk+rls);
    setTimeout(function(){
      vco = null;
      mod = null;
      envelope = null;
    },atk+rls+1);
    
    envelope.trigger();
  }
  function noiseOn(){
    // Stop if already playing:
    noiseOff();
    // Randomly modulate playback of white noise with sine
    // The sine wave:
    noiseSine = new VCO(context, 'sine', 1, 1);
    noiseSine.setFrequency(Math.random()*4000);
    noiseSine.oscillator.start(context.currentTime);

    // connect sine to whiteNoise playback speed
    whiteNoise = new WhiteNoise(context, 0.25);
    whiteNoise.node.start(context.currentTime);
    noiseSine.connect(whiteNoise.node.playbackRate);
    // connect noise to filter
    whiteNoise.connect(biquadFilter);
    // connect filter to output
    biquadFilter.connect(globalGain);
    $("#jellyfishicon").trigger('startRumble');
    rumbling = true;
  }
  function noiseOff(){
    if(whiteNoise!=null){
      noiseSine.oscillator.stop(context.currentTime);
      whiteNoise.node.stop(context.currentTime);
      whiteNoise.disconnect();
      whiteNoise = null;
    }
    if(rumbling){
      $("#jellyfishicon").trigger('stopRumble');
      rumbling = false;
    }
  }
  function bubblesOn(){
    // Stop if already playing:
    bubblesOff();
    // Modulate playback of sine wave with saw
    // The sine wave:
    bubbleSine = new VCO(context, 'sine', 200, 0.4);
    bubbleFreq = 10+(Math.random()*500);
    bubbleSine.setFrequency(bubbleFreq);
    bubbleSine.setGain(0.7);
    bubbleSine.oscillator.start(context.currentTime);
    bubblesPlaying = true;
    clearTimeout(bubbleEvent);
    clearTimeout(bubbleAnimation);
    processBubbles();
    animateBubbles();
    bubbleSine.connect(globalGain);
    bubbling=true;
    jellyGoRight();
  }


  function animateBubbles(){
    var bbl = $("<img class='bubbleimg' src='img/bubble.png' style='top: 100%; left: "+(Math.random()*100)+"%'>");
    bbl.appendTo($("#jellycontainer"));
    bbl.animate({
        top: "-20",
      }, 800+(bubbleAnimationRate*4),"linear",function(){$(this).remove()});
    bubbleAnimation = setTimeout(animateBubbles,bubbleAnimationRate*4);
  }

  function processBubbles(){
    // cancel scheduled values
    bubbleSine.oscillator.frequency.cancelScheduledValues(context.currentTime);
    // set frequency values for the next 200 ms
    for(i = 0; i<110; i++){
      bubbleFreq = bubbleFreq*bubbleRate;
      if(bubbleFreq > 2000+bubbleFreqOffset){
        bubbleFreq = 10+(Math.random()*500)+bubbleFreqOffset;
        if(bubbleFreq <= 0){
          bubbleFreq = 10;
        }
      }
      // set the value
      bubbleSine.setFrequency(bubbleFreq, context.currentTime+(i/1000));
      // Hack for mobile: set gain of last bubble to 0
      //bubbleSine.setGain(0, context.currentTime+0.1);
    }
    bubbleEvent = setTimeout(processBubbles,100);
  }

  function jellyGoRight(){
    if(bubbling){
      $("#jellyfishicon").animate({left: "10"},300,"swing",jellyGoLeft); 
    }
  }
  function jellyGoLeft(){
    if(bubbling){
      $("#jellyfishicon").animate({left: "-10"},300,"swing",jellyGoRight);
    } 
  }
  function bubblesOff(){
    if(bubbling){
      bubbling = false;
      $("#jellyfishicon").stop(true);
      clearTimeout(bubbleEvent);
      clearTimeout(bubbleAnimation);
      // fade out
      fadeBubbles();
    }
  }

  function fadeBubbles(){
    if(bubbleSine!=null){
      if ((bubbleSine.gain.gain.value <= 0) && bubblesPlaying){
        bubbleSine.oscillator.stop(context.currentTime);
        bubblesPlaying = false;
      }else{
        // cancel scheduled values...
        bubbleSine.gain.gain.cancelScheduledValues(context.currentTime);
        var bubbleGain = bubbleSine.gain.gain.value;
        // set gain for the next 100 ms
        for(i = 0; i<100; i++){
          bubbleGain = Math.max(bubbleGain-0.01,0);
          bubbleSine.setGain(bubbleGain, context.currentTime+(i/1000));
        }
        if(bubblesPlaying){
          bubbleFade = setTimeout(fadeBubbles,100);
        }
      }
    }
  }

  function drums(){
    var playSound = context.createBufferSource();
    var bufNum;
    if(timesPressed%8 == 0 || timesPressed%8 == 3 || timesPressed%8 == 5){
      if(Math.random()<0.9){ // 90% of the time, KICK!
        bufNum = 0;
      }else{
        bufNum = Math.floor(Math.random()*soundBuffers.length);
      }
    }else if(timesPressed%4 == 2){
      bufNum = 1;
    }else{
      if(Math.random()>0.95){ // 5% of the time, KICK!
        bufNum = 0;
      }else{ // 95% of the time, NO KICK!
        bufNum = 1+Math.floor(Math.random()*(soundBuffers.length-1));
      }
    }
    if(soundBuffers[bufNum]==null){
      console.log("Error: empty sound buffer "+bufNum);
    }else{
      playSound.buffer = soundBuffers[bufNum]; // Attach buffer
      var drumsGainNode = context.createGain();
      drumsGainNode.gain.value = 3;
      playSound.connect(drumsGainNode);
      drumsGainNode.connect(globalGain); 
      playSound.start(0);
      // animate
      $('#jellyfishicon').attr('src', "img/jellyfish-icon-swimming.png");
      // animate img
      $( "#jellyfishicon" ).animate({
        top: "-=10",
      }, 80, function(){
        $( "#jellyfishicon" ).animate({
          top: "10px",
        }, 80).attr('src', "img/jellyfish-icon-straight.png");
      });
      timesPressed = (timesPressed + 1)%16;
    }
  }

  function brokenDrums(){
    var playSound = context.createBufferSource();
    var bufNum;
    // random buffer
    bufNum = Math.floor(Math.random()*soundBuffers.length);
    if(soundBuffers[bufNum]==null){
      console.log("Error: empty sound buffer "+bufNum);
    }else{
      playSound.buffer = soundBuffers[bufNum]; // Attach buffer
      var rate = 1;
      // 20% faster, 80% slower
      if(Math.random() > 0.8){
          rate = (2+Math.random()*16);
      }else{
          rate = (0.001+(Math.random()*0.5));
      }
      playSound.playbackRate.value = (rate); // random playback rate
      var drumsGainNode = context.createGain();
      drumsGainNode.gain.value = 3;
      playSound.connect(drumsGainNode);
      drumsGainNode.connect(globalGain); 
      playSound.start(0);
      // animate
      $('#jellyfishicon').attr('src', "img/jellyfish-icon-swimming.png");
      // animate img
      $( "#jellyfishicon" ).animate({
        top: "-=10",
      }, 80, function(){
        $( "#jellyfishicon" ).animate({
          top: "10px",
        }, 80).attr('src', "img/jellyfish-icon-straight.png");
      });
    }
  }

  function loopy(){
    // Stop if already playing:
    loopOff();
    // Play loopy saw wave patterns
    // The saw wave:
    loopySaw = new VCO(context, 'square', 200, 0.4);
    // connect saw to VCA 
    loopySaw.connect(loopyVCA);

    
    // start oscillator
    loopySaw.oscillator.start(context.currentTime);
    if(loopyTimeout != null){
      clearTimeout(loopyTimeout);
    }
    if(loopyAnimation != null){
      clearTimeout(loopyAnimation);
    }
    animateLoopy();
    doLoopy();
  }

  function loopOff(){
    if(loopyAnimation != null){
      clearTimeout(loopyAnimation);
    }
    $("#jellyfishicon").stop(true);
    if(loopySaw!=null){
      loopySaw.oscillator.stop(context.currentTime);
      loopySaw = null;
    }
  }

  function doLoopy(){
    // cancel scheduled values
    loopySaw.oscillator.frequency.cancelScheduledValues(context.currentTime);
    // set next freqs.length frequency value changes
    // Time based on loopyDur
    var timeOffset = 0;
    var currentGain = loopySaw.gain.gain.value;
    for(i = 0; i<(freqs.length); i++){
      var loopFreq = loopyFreqs[i%freqs.length];
      loopySaw.setFrequency(loopFreq, context.currentTime+timeOffset);
      loopyEnvelope.trigger(context.currentTime+timeOffset);
      timeOffset += loopyDurs[i];
      if(fadingLoopy){
        currentGain -= 0.008;
        loopySaw.setGain(currentGain, context.currentTime+timeOffset);
      }
    }
    if(loopySaw.gain.gain.value >= 0.001){
      loopyTimeout = setTimeout(doLoopy,(loopyTotalDur*1000));
    }else{
      loopOff();
    }
  }

  function animateLoopy(){
    $( "#jellyfishicon" ).animate({
      top: "+=10",
    }, 60);
    $( "#jellyfishicon" ).animate({
      top: "0",
    }, 120);
    loopyAnimation = setTimeout(animateLoopy,loopyTotalDur*1000);
  }

  function resetLoopyFreqs(){
    freqIndexArray = [];
    for(i=0; i<freqs.length; i++){
      freqIndexArray[i] = i;
    }
    freqIndexArray = shuffleArray(freqIndexArray);
    lastIndex = freqIndexArray[freqIndexArray.length-1];
    if(loopySaw != null){
      if(Math.random()>0.5){
        loopySaw.oscillator.type = 'square';
      }else{
        loopySaw.oscillator.type = 'sawtooth';
      }
    }      
    loopyFreqs = freqs; //shuffleArray(freqs);
    loopyDurs = [];
    loopyTotalDur = 0;
    var dur = [0.16,0.16,(0.16*3/2),0.16][Math.floor(Math.random()*4)];
    if(freqs.length <= 1){
      dur = 0.16;
    }
    for(var i=0; i<(loopyFreqs.length); i++){
      loopyDurs[i] = dur;
      loopyTotalDur += dur;
    }
  }

  // Like long tones, but always on
  function oneNote(){
    // change img
    $('#jellyfishicon').attr('src', "img/jellyfish-icon-white.png");
    oneNoteOff();
    // make new synth, envelope
    oneNoteOsc = new VCO(context, 'sawtooth', myFreq, 0.0001);
    // make new biquad filter
    biquadFilter = context.createBiquadFilter();
    biquadFilter.type = "lowpass";
    biquadFilter.frequency.value = 1000;
    biquadFilter.Q.value = 10;
    // connect synth to filter
    oneNoteOsc.connect(biquadFilter);
    // connect filter to output
    biquadFilter.connect(globalGain);
    // start oscillator
    oneNoteOsc.oscillator.start(context.currentTime);
    oneNoteGainLoop();
  }

  function oneNoteOff(){
    if(oneNoteOsc!=null){
      oneNoteOsc.oscillator.stop(context.currentTime);
    }
  }

  // Hacky function to make volume swells up and down
  function oneNoteGainLoop(){
    // cancel scheduled values
    if (oneNoteTimeout != null){
      clearTimeout(oneNoteTimeout);
    }
    oneNoteOsc.gain.gain.cancelScheduledValues(context.currentTime);
    var currentGain = oneNoteOsc.gain.gain.value;
    if(!fadingInOneNote){
      $( "#jellyfishicon" ).css("opacity",currentGain);
    }
    // set next 100 gains 
    for(i = 0; i<100; i++){
      oneNoteOsc.setGain(currentGain, context.currentTime+(i/1000));
      if(fadingInOneNote){
        currentGain = currentGain*1.001;
      }else{
        currentGain = currentGain+oneNoteGainChange;
      }
      if(currentGain >= 0.3){
        oneNoteGainChange = -0.0001;
        fadingInOneNote = false;
      }else if(currentGain <= 0.01){
        oneNoteGainChange = 0.0001;
      }
    }
    oneNoteTimeout = setTimeout(oneNoteGainLoop,100);
  }

  // Code to create pulse waves
  //Pre-calculate the WaveShaper curves so that we can reuse them.
  var pulseCurve=new Float32Array(256);
  for(var i=0;i<128;i++) {
    pulseCurve[i]= -1;
    pulseCurve[i+128]=1;
  }
  var constantOneCurve=new Float32Array(2);
  constantOneCurve[0]=1;
  constantOneCurve[1]=1;

  //Add a new factory method to the AudioContext object.
  context.createPulseOscillator=function(){
    //Use a normal oscillator as the basis of our new oscillator.
    var node=this.createOscillator();
    node.type="sawtooth";

    //Shape the output into a pulse wave.
    var pulseShaper=ac.createWaveShaper();
    pulseShaper.curve=pulseCurve;
    node.connect(pulseShaper);

    //Use a GainNode as our new "width" audio parameter.
    var widthGain=ac.createGain();
    widthGain.gain.value=0; //Default width.
    node.width=widthGain.gain; //Add parameter to oscillator node.
    widthGain.connect(pulseShaper);

    //Pass a constant value of 1 into the widthGain – so the "width" setting
    //is duplicated to its output.
    var constantOneShaper=this.createWaveShaper();
    constantOneShaper.curve=constantOneCurve;
    node.connect(constantOneShaper);
    constantOneShaper.connect(widthGain);

    //Override the oscillator's "connect" and "disconnect" method so that the
    //new node's output actually comes from the pulseShaper.
    node.connect=function() {
      pulseShaper.connect.apply(pulseShaper, arguments);
    }
    node.disconnect=function() {
      pulseShaper.disconnect.apply(pulseShaper, arguments);
    }

    return node;
  }
});
