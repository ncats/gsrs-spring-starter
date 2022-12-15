#!/usr/bin/env perl

use strict;
use warnings;
use XML::MyXML qw(:all);
use Test::More;
use JSON;
use Path::Tiny;

# Enter the current gsrs deployment version
my $gdv = '3.0.3';
my $gdvsnapshot = $gdv.'-SNAPSHOT';

# These checks are peformed before deployment 
# To make sure pom version information is correct
# and we have some needed files

print "^^^ Checking gsrs gsrs-spring-starter ^^^\n\n";
print "The script is assuming the version $gdvsnapshot\n";
print "Edit file if needed.\n\n";


my $installExtraJars_script_text = path('./installExtraJars.sh')->slurp_utf8();

my $root_obj = xml_to_object('.' . '/pom.xml', {file=>1});    
print "=== root ===\n";
ok($root_obj ->path('/project/properties/gsrs.version')->text eq $gdvsnapshot, 'gsrs.version'); 
print "\n";

my @modules = $root_obj->path('/project/modules/');
for my $module (@modules) {
  my $project = $module->text;  
  if ($project eq 'gsrs-discovery') { 
    my $obj = xml_to_object($project . '/pom.xml', {file=>1}); 
    print "=== $project ===\n";
    ok($obj->path('/project/version')->text eq $gdvsnapshot, '(discovery) -- version'); 
    print "\n";
  } else { 
    my $obj = xml_to_object($project . '/pom.xml', {file=>1}); 
    print "=== $project ===\n";
    ok($obj->path('/project/parent/version')->text eq $gdvsnapshot, 'parent -- version'); 
    print "\n";
    
    
    if ($project eq 'gsrs-spring-legacy-structure-indexer') {
      for my $dep ($obj->path('/project/dependencies/')) {    
        if ($dep->path('groupId')->text eq 'gov.nih.ncats' and $dep->path('artifactId')->text eq 'structure-indexer') {
          my $jar_file = 'extraJars/'.$dep->path('artifactId')->text . '-' . $dep->path('version')->text .'.jar';
          ok (-f $jar_file,  'structure indexer dependency');                    
          ok(find_jar_in_installExtraJars_script_text($jar_file), 'structure indexer dependency jar file in installExtraJars.sh'); 
        }
      }
      print "\n";
    }
  }
  
  sub find_jar_in_installExtraJars_script_text { 
    my $filename = shift;
    die "jar $filename must be defined\n" if (!$filename);    
    return ($installExtraJars_script_text =~ m/\Q$filename/);
  }        
  
}

# Questions: 
   # why are cdk and structure indexer part of starter and not substances? 
   # https://cdk.github.io/
   # how to check cdk files ? 
   # cdk is currently at version 2.8, should we be using 2.6.

done_testing();


__END__


