#!usr/bin/perl -w

use strict;

die "usage:perl $0 <fpkm.matrix> <low fpkm cutoff>\n" unless @ARGV == 2;

open A,$ARGV[0];
my $head=<A>;
print "$head";
while(<A>){
    chomp;my @a=split;my $num=0;
    foreach my $k(1..$#a){
        if($a[$k] > $ARGV[1]){
            $num++;
        }
    }
    next if($num == 0);
    print "$_\n";
}
close A;
