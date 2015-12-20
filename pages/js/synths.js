var context = new AudioContext();
var VCO = (function(context) {
  function VCO(type, freq, gain){
    this.oscillator = context.createOscillator();
    this.oscillator.type = type;
    this.setFrequency(freq);
    this.gain = context.createGain();
    this.gain.gain.value = gain;
    this.oscillator.connect(this.gain);

    this.input = this.oscillator;
    this.output = this.oscillator;

    var that = this;
    $(document).bind('frequency', function (_, frequency) {
      that.setFrequency(frequency);
    });
  };

  VCO.prototype.setFrequency = function(frequency) {
    this.oscillator.frequency.setValueAtTime(frequency, context.currentTime);
  };

  VCO.prototype.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  }

  return VCO;
})(context);

var AR_EnvelopeGenerator = (function(context) {
  function AR_EnvelopeGenerator(peak) {
    this.attackTime = 0.1;
    this.releaseTime = 0.1;
    this.peak = peak;
    var that = this;
    $(document).bind('gateOn', function (_) {
      that.trigger();
    });
    $(document).bind('setAttack', function (_, value) {
      that.attackTime = value;
    });
    $(document).bind('setRelease', function (_, value) {
      that.releaseTime = value;
    });
  };

  AR_EnvelopeGenerator.prototype.trigger = function() {
    now = context.currentTime;
    this.param.cancelScheduledValues(now);
    this.param.setValueAtTime(0, now);
    this.param.linearRampToValueAtTime(this.peak, now + this.attackTime);
    this.param.linearRampToValueAtTime(0, now + this.attackTime + this.releaseTime);
  };

  AR_EnvelopeGenerator.prototype.connect = function(param) {
    this.param = param;
  };

  return AR_EnvelopeGenerator;
})(context);


var VCA = (function(context) {
  function VCA() {
    this.gain = context.createGain();
    this.gain.gain.value = 0;
    this.input = this.gain;
    this.output = this.gain;
    this.amplitude = this.gain.gain;
  };

  VCA.prototype.connect = function(node) {
    if (node.hasOwnProperty('input')) {
      this.output.connect(node.input);
    } else {
      this.output.connect(node);
    };
  }

  return VCA;
})(context);