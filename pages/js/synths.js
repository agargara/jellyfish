function VCO(context, type, freq, gain){
  this.oscillator = context.createOscillator();
  this.oscillator.type = type;
  
  this.gain = context.createGain();
  this.gain.gain.value = gain;

  this.input = this.oscillator;
  this.output = this.oscillator;

  this.setFrequency = function(freq) {
    this.oscillator.frequency.setValueAtTime(freq, context.currentTime);
  };

  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  };

  this.setFrequency(freq);
  this.oscillator.connect(this.gain);
};

function VCA(context) {
  this.gain = context.createGain();
  this.gain.gain.value = 0;
  this.input = this.gain;
  this.output = this.gain;
  this.amplitude = this.gain.gain;

  this.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  }
};

function AR_EnvelopeGenerator(context, peak) {
  this.attackTime = 0.1;
  this.releaseTime = 0.1;
  this.peak = peak;

  this.trigger = function() {
    now = context.currentTime;
    this.param.cancelScheduledValues(now);
    this.param.setValueAtTime(0, now);
    this.param.linearRampToValueAtTime(this.peak, now + this.attackTime);
    this.param.linearRampToValueAtTime(0, now + this.attackTime + this.releaseTime);
  };

  this.connect = function(param) {
    this.param = param;
  };
};