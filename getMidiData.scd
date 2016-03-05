

m = SimpleMIDIFile.read( "/Users/agargara/programming/rhizome/jellyfish/sounds/jellyfish-melody-section-chords.mid"); // create empty file

(
var time = 0;
var array = [];
m.midiEvents.do{
  |m|
  if((m[2] == \noteOn),{
    if(m[1] > time,{
      time = m[1];
      array = array ++ ((array+12)); // add octaves
      array = array.as(Set).as(Array);        // convert to set and back to remove dups
      array.sort; // sort
      array.postln;
      array = [];
    });
    array = array.add(m[4]);
  });
};
)