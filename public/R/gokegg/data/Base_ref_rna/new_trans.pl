#!usr/bin/perl -w

use strict;

die "usage:perl $0 <refmap> <merge.gtf> \n" unless @ARGV == 2;

open A,$ARGV[0];    open B,$ARGV[1];    my %id;
<A>;
while(<A>){
    chomp;my @a=split;  my @b=split /\|/,$a[-1];$id{$b[1]}=1;
}
close A;

while(<B>){
    chomp;my @a=split;
    my $id=$a[17];
    $id=~s/[";]//g;
    #print "$id\n";
    if(!exists $id{$id}){
        print "$_\n";
    }
}
close B;
