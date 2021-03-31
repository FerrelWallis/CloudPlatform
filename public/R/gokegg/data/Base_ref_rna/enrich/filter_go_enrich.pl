#!usr/bin/perl -w

use strict;

die "usage:perl $0 <enrich info> <filter p value> <enrich output> \n" unless @ARGV  == 3;

open A,$ARGV[0];    open OUT,">$ARGV[2]"; 

my $head=<A>;	chomp $head; my @head=split /\t/,$head; my $ab1=join "\t",@head[0..5]; my $ab2=join "\t",@head[9..$#head];
print OUT "$ab1\t$ab2\n";
while(<A>){
    chomp;my @a=split /\t/;
    next if($a[5] > $ARGV[1]);    next if($a[1] eq "p");
    my $mm1=join "\t",@a[0..5];	my $mm2=join "\t",@a[9..$#a];
    print OUT "$mm1\t$mm2\n";
}
close A;    close OUT;
